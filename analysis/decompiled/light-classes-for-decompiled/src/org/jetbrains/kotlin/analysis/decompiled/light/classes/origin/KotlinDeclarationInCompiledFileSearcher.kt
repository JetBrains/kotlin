/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.origin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.decompiler.psi.text.getAllModifierLists
import org.jetbrains.kotlin.analysis.decompiler.psi.text.getQualifiedName
import org.jetbrains.kotlin.asJava.elements.psiType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.constant.StringValue
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.name.JvmNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl
import org.jetbrains.kotlin.type.MapPsiToAsmDesc
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class KotlinDeclarationInCompiledFileSearcher {
    abstract fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember, signature: MemberSignature): KtDeclaration?
    fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember): KtDeclaration? {
        val signature = when (member) {
            is PsiField -> {
                val desc = MapPsiToAsmDesc.typeDesc(member.type)
                MemberSignature.fromFieldNameAndDesc(member.name, desc)
            }

            is PsiMethod -> {
                val desc = MapPsiToAsmDesc.methodDesc(member)
                val name = if (member.isConstructor) "<init>" else member.name
                MemberSignature.fromMethodNameAndDesc(name, desc)
            }

            else -> null
        } ?: return null

        return findDeclarationInCompiledFile(file, member, signature)
    }

    protected fun findByStubs(
        file: KtClsFile,
        relativeClassName: List<Name>,
        member: PsiMember,
        memberName: String,
    ): KtDeclaration? {
        val classOrFile: KtDeclarationContainer = file.declarations.singleOrNull() as? KtClassOrObject ?: file
        val container: KtDeclarationContainer = if (relativeClassName.isEmpty())
            classOrFile
        else {
            relativeClassName.fold(classOrFile) { declaration: KtDeclarationContainer?, name: Name ->
                declaration?.declarations?.singleOrNull { it is KtClassOrObject && it.name == name.asString() } as? KtClassOrObject
            }
        } ?: return null

        if (member is PsiMethod && member.isConstructor) {
            return container.safeAs<KtClassOrObject>()
                ?.takeIf { it.name == memberName }
                ?.allConstructors
                ?.firstOrNull { doParametersMatch(member, it) }
        }

        val declarations = container.declarations
        return when (member) {
            is PsiMethod -> {
                val names = SmartList(memberName)
                val setter = if (JvmAbi.isGetterName(memberName) && !PsiType.VOID.equals(member.returnType)) {
                    propertyNameByGetMethodName(Name.identifier(memberName))?.let { names.add(it.identifier) }
                    false
                } else if (JvmAbi.isSetterName(memberName) && PsiType.VOID.equals(member.returnType)) {
                    propertyNamesBySetMethodName(Name.identifier(memberName)).forEach { names.add(it.identifier) }
                    true
                } else true
                declarations
                    .firstOrNull { declaration ->
                        nameMatches(declaration, names) &&
                                (declaration is KtNamedFunction && doParametersMatch(member, declaration) ||
                                        declaration is KtProperty && doPropertyMatch(member, declaration, setter))
                    }
            }

            is PsiField -> {
                if (container is KtObjectDeclaration && memberName == "INSTANCE") {
                    return container
                }
                declarations.singleOrNull { it !is KtNamedFunction && it.name == memberName }
            }
            else -> declarations.singleOrNull { it.name == memberName }
        }
    }

    private fun nameMatches(declaration: KtDeclaration?, names: MutableList<String>): Boolean {
        if (getJvmName(declaration) in names) return true
        return declaration is KtProperty && (getJvmName(declaration.getter) in names || getJvmName(declaration.setter) in names)
    }

    private fun getJvmName(declaration: KtDeclaration?): String? {
        if (declaration == null) return null
        val annotationEntry = declaration.annotationEntries.firstOrNull {
            it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == JvmNames.JVM_NAME_SHORT
        }
        if (annotationEntry != null) {
            val constantValue = (annotationEntry.stub as? KotlinAnnotationEntryStubImpl)?.valueArguments?.get(Name.identifier("name"))
            if (constantValue is StringValue) {
                return constantValue.value
            }
        }
        return declaration.name
    }

    private fun doPropertyMatch(member: PsiMethod, property: KtProperty, setter: Boolean): Boolean {
        val ktTypes = mutableListOf<KtTypeReference>()
        property.contextReceivers.forEach { ktTypes.add(it.typeReference()!!) }
        property.receiverTypeReference?.let { ktTypes.add(it) }
        property.typeReference?.let { ktTypes.add(it) }

        val psiTypes = mutableListOf<PsiType>()
        member.parameterList.parameters.forEach { psiTypes.add(it.type) }
        if (!setter) {
            val returnType = member.returnType ?: return false
            psiTypes.add(returnType)
        }

        if (ktTypes.size != psiTypes.size) return false
        ktTypes.zip(psiTypes).forEach { (ktType, psiType) ->
            if (!areTypesTheSame(ktType, psiType, false)) return false
        }
        return true
    }

    private fun doParametersMatch(member: PsiMethod, ktNamedFunction: KtFunction): Boolean {
        if (!doTypeParameters(member, ktNamedFunction)) {
            return false
        }
        val ktTypes = mutableListOf<KtTypeReference>()
        ktNamedFunction.contextReceivers.forEach { ktTypes.add(it.typeReference()!!) }
        ktNamedFunction.receiverTypeReference?.let { ktTypes.add(it) }
        val parametersCount = member.parameterList.parametersCount
        val isJvmOverloads = ktNamedFunction.annotationEntries.any {
            it.calleeExpression?.constructorReferenceExpression?.getReferencedName() ==
                    JvmNames.JVM_OVERLOADS_FQ_NAME.shortName().asString()
        }
        val firstDefaultParametersToPass = if (isJvmOverloads) {
            val totalNumberOfParametersWithDefaultValues = ktNamedFunction.valueParameters.filter { it.hasDefaultValue() }.size
            val numberOfSkippedParameters = ktNamedFunction.valueParameters.size + ktTypes.size - parametersCount
            totalNumberOfParametersWithDefaultValues - numberOfSkippedParameters
        } else 0
        var defaultParamIdx = 0
        for (valueParameter in ktNamedFunction.valueParameters) {
            if (isJvmOverloads && valueParameter.hasDefaultValue()) {
                if (defaultParamIdx >= firstDefaultParametersToPass) {
                    continue
                }
                defaultParamIdx++
            }

            ktTypes.add(valueParameter.typeReference!!)
        }
        if (parametersCount != ktTypes.size) return false
        member.parameterList.parameters.map { it.type }
            .zip(ktTypes)
            .forEach { (psiType, ktTypeRef) ->
                if (!areTypesTheSame(ktTypeRef, psiType, (ktTypeRef.parent as? KtParameter)?.isVarArg == true)) return false
            }
        return true
    }

    private fun doTypeParameters(member: PsiMethod, ktNamedFunction: KtFunction): Boolean {
        if (member.typeParameters.size != ktNamedFunction.typeParameters.size) return false
        val boundsByName = ktNamedFunction.typeConstraints.groupBy { it.subjectTypeParameterName?.getReferencedName() }
        member.typeParameters.zip(ktNamedFunction.typeParameters) { psiTypeParam, ktTypeParameter ->
            if (psiTypeParam.name.toString() != ktTypeParameter.name) return false
            val psiBounds = mutableListOf<KtTypeReference>()
            psiBounds.addIfNotNull(ktTypeParameter.extendsBound)
            boundsByName[ktTypeParameter.name]?.forEach {
                psiBounds.addIfNotNull(it.boundTypeReference)
            }
            val expectedBounds = psiTypeParam.extendsListTypes
            if (psiBounds.size != expectedBounds.size) return false
            expectedBounds.zip(psiBounds) { expectedBound, candidateBound ->
                if (!areTypesTheSame(candidateBound, expectedBound, false)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Compare erased types
     */
    private fun areTypesTheSame(ktTypeRef: KtTypeReference, psiType: PsiType, varArgs: Boolean): Boolean {
        val qualifiedName =
            getQualifiedName(ktTypeRef.typeElement, ktTypeRef.getAllModifierLists().any { it.hasSuspendModifier() }) ?: return false
        if (psiType is PsiArrayType && psiType.componentType !is PsiPrimitiveType) {
            return qualifiedName == StandardNames.FqNames.array.asString() ||
                    varArgs && areTypesTheSame(ktTypeRef, psiType.componentType, false)
        }
        //currently functional types are unresolved and thus type comparison doesn't work
        return psiType.canonicalText.takeWhile { it != '<' } == psiType(qualifiedName, ktTypeRef).canonicalText
    }

    companion object {
        fun getInstance(): KotlinDeclarationInCompiledFileSearcher =
            ApplicationManager.getApplication().getService(KotlinDeclarationInCompiledFileSearcher::class.java)
    }
}