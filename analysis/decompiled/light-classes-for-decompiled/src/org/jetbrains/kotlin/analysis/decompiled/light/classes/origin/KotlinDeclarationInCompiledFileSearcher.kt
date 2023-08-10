/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.origin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
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
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.allConstructors
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
                } else null
                declarations
                    .firstOrNull { declaration ->
                        val declarationName = getJvmName(declaration)
                        when (declaration) {
                            is KtNamedFunction -> {
                                declarationName in names && doParametersMatch(member, declaration)
                            }
                            is KtProperty -> {
                                val getterName = getJvmName(declaration.getter)
                                val setterName = getJvmName(declaration.setter)
                                if (setter != null) {
                                    val accessorName = (if (setter) setterName else getterName) ?: declarationName
                                    accessorName in names && doPropertyMatch(member, declaration, setter)
                                } else {
                                    getterName in names && doPropertyMatch(member, declaration, false) ||
                                            setterName in names && doPropertyMatch(member, declaration, true)
                                }
                            }
                            else -> false
                        }
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

    private fun getJvmName(declaration: KtDeclaration?): String? {
        if (declaration == null) return null
        val annotationEntry = declaration.annotationEntries.firstOrNull {
            it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == JvmStandardClassIds.JVM_NAME_SHORT
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
        val valueParameters = ktNamedFunction.valueParameters
        val memberParameterList = member.parameterList
        val memberParametersCount = memberParameterList.parametersCount
        val parametersCount = memberParametersCount - (if (ktNamedFunction.isSuspendFunction(memberParameterList)) 1 else 0)
        val isJvmOverloads = ktNamedFunction.annotationEntries.any {
            it.calleeExpression?.constructorReferenceExpression?.getReferencedName() ==
                    JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME.shortName().asString()
        }
        val firstDefaultParametersToPass = if (isJvmOverloads) {
            val totalNumberOfParametersWithDefaultValues = valueParameters.filter { it.hasDefaultValue() }.size
            val numberOfSkippedParameters = valueParameters.size + ktTypes.size - parametersCount
            totalNumberOfParametersWithDefaultValues - numberOfSkippedParameters
        } else 0
        var defaultParamIdx = 0
        for (valueParameter in valueParameters) {
            if (isJvmOverloads && valueParameter.hasDefaultValue()) {
                if (defaultParamIdx >= firstDefaultParametersToPass) {
                    continue
                }
                defaultParamIdx++
            }

            ktTypes.add(valueParameter.typeReference!!)
        }
        if (parametersCount != ktTypes.size) return false
        memberParameterList.parameters.map { it.type }
            .zip(ktTypes)
            .forEach { (psiType, ktTypeRef) ->
                if (!areTypesTheSame(ktTypeRef, psiType, (ktTypeRef.parent as? KtParameter)?.isVarArg == true)) return false
            }
        return true
    }

    private fun KtFunction.isSuspendFunction(memberParameterList: PsiParameterList): Boolean {
        if (modifierList?.hasSuspendModifier() != true || memberParameterList.isEmpty) return false

        val memberParametersCount = memberParameterList.parametersCount
        val continuationPsiType = psiType(StandardNames.CONTINUATION_INTERFACE_FQ_NAME.asString(), this)
        val memberType = memberParameterList.getParameter(memberParametersCount - 1)?.type ?: return false
        // check fqName ignoring generic parameter
        return memberType.isTheSame(continuationPsiType)
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
        return if (psiType is PsiArrayType && psiType.componentType !is PsiPrimitiveType) {
            qualifiedName == StandardNames.FqNames.array.asString() ||
                    varArgs && areTypesTheSame(ktTypeRef, psiType.componentType, false)
        } else {
            psiType.isTheSame(psiType(qualifiedName, ktTypeRef))
        }
    }

    private fun PsiType.isTheSame(psiType: PsiType): Boolean =
        //currently functional types are unresolved and thus type comparison doesn't work
        canonicalText.takeWhile { it != '<' } == psiType.canonicalText

    companion object {
        fun getInstance(): KotlinDeclarationInCompiledFileSearcher =
            ApplicationManager.getApplication().getService(KotlinDeclarationInCompiledFileSearcher::class.java)
    }
}