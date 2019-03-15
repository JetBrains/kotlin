/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightFieldBuilder
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightSimpleModifierList
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.VOLATILE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType

internal open class KtUltraLightField(
    protected val declaration: KtNamedDeclaration,
    name: String,
    private val containingClass: KtUltraLightClass,
    private val support: UltraLightSupport,
    modifiers: Set<String>
) : LightFieldBuilder(name, PsiType.NULL, declaration), KtLightField,
    KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiField> {
    private val modList = object : KtLightSimpleModifierList(this, modifiers) {
        override fun hasModifierProperty(name: String): Boolean = when (name) {
            PsiModifier.VOLATILE -> hasFieldAnnotation(VOLATILE_ANNOTATION_FQ_NAME)
            PsiModifier.TRANSIENT -> hasFieldAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)
            else -> super.hasModifierProperty(name)
        }

        private fun hasFieldAnnotation(fqName: FqName): Boolean {
            val annotation = support.findAnnotation(declaration, fqName)?.first ?: return false
            val target = annotation.useSiteTarget?.getAnnotationUseSiteTarget() ?: return true
            val expectedTarget =
                if (declaration is KtProperty && declaration.hasDelegate()) AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
                else AnnotationUseSiteTarget.FIELD
            return target == expectedTarget
        }
    }

    override fun getModifierList(): PsiModifierList = modList
    override fun hasModifierProperty(name: String): Boolean =
        modifierList.hasModifierProperty(name) //can be removed after IDEA platform does the same

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    private val propertyDescriptor: PropertyDescriptor? by lazyPub {
        declaration.resolve() as? PropertyDescriptor
    }

    private val kotlinType: KotlinType? by lazyPub {
        when {
            declaration is KtProperty && declaration.hasDelegate() ->
                propertyDescriptor
                    ?.let {
                        val context = LightClassGenerationSupport.getInstance(project).analyze(declaration)
                        PropertyCodegen.getDelegateTypeForProperty(declaration, it, context)
                    }
            declaration is KtObjectDeclaration ->
                (declaration.resolve() as? ClassDescriptor)?.defaultType
            declaration is KtEnumEntry -> {
                (containingClass.kotlinOrigin.resolve() as? ClassDescriptor)?.defaultType
            }
            else -> {
                declaration.getKotlinType()
            }
        }
    }

    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        // We don't generate nullability annotations for non-backing fields in backend
        get() = kotlinType?.takeUnless { declaration is KtEnumEntry || declaration is KtObjectDeclaration }

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = type

    private val _type: PsiType by lazyPub {
        fun nonExistent() = JavaPsiFacade.getElementFactory(project).createTypeFromText("error.NonExistentClass", declaration)

        when {
            (declaration is KtProperty && declaration.hasDelegate()) || declaration is KtEnumEntry || declaration is KtObjectDeclaration ->
                kotlinType?.asPsiType(support, TypeMappingMode.DEFAULT, this)
                    ?.let(TypeConversionUtil::erasure)
                    ?: nonExistent()
            else -> {
                val kotlinType = declaration.getKotlinType() ?: return@lazyPub PsiType.NULL
                val descriptor = propertyDescriptor ?: return@lazyPub PsiType.NULL

                support.mapType(this) { typeMapper, sw ->
                    typeMapper.writeFieldSignature(kotlinType, descriptor, sw)
                }
            }
        }
    }

    override fun getType(): PsiType = _type

    override fun getParent() = containingClass
    override fun getContainingClass() = containingClass
    override fun getContainingFile(): PsiFile? = containingClass.containingFile

    override fun computeConstantValue(): Any? =
        if (hasModifierProperty(PsiModifier.FINAL) &&
            (TypeConversionUtil.isPrimitiveAndNotNull(_type) || _type.equalsToText(CommonClassNames.JAVA_LANG_STRING))
        )
            (declaration.resolve() as? VariableDescriptor)?.compileTimeInitializer?.value
        else null

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? = computeConstantValue()

    override val kotlinOrigin = declaration
    override val clsDelegate: PsiField
        get() = throw IllegalStateException("Cls delegate shouldn't be loaded for ultra-light PSI!")
    override val lightMemberOrigin = LightMemberOriginForDeclaration(declaration, JvmDeclarationOriginKind.OTHER)

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtNamedDeclaration)?.setName(name)
        return this
    }

    override fun setInitializer(initializer: PsiExpression?) = cannotModify()

}

internal class KtUltraLightEnumEntry(
    declaration: KtEnumEntry,
    name: String,
    containingClass: KtUltraLightClass,
    support: UltraLightSupport,
    modifiers: Set<String>
) : KtUltraLightField(declaration, name, containingClass, support, modifiers), PsiEnumConstant {
    private val enumEntry get() = declaration as KtEnumEntry

    private val _initializingClass by lazyPub {
        if (enumEntry.body != null)
            KtUltraLightClassForEnumEntry(enumEntry, containingClass.support, this)
        else
            null
    }

    override fun getInitializingClass(): PsiEnumConstantInitializer? = _initializingClass
    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        _initializingClass ?: error("cannot create initializing class in light enum constant")

    override fun getArgumentList(): PsiExpressionList? = null
    override fun resolveMethod(): PsiMethod? = null
    override fun resolveConstructor(): PsiMethod? = null

    override fun resolveMethodGenerics(): JavaResolveResult = JavaResolveResult.EMPTY

    override fun hasInitializer() = true
    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?) = this
}

internal class KtUltraLightClassForEnumEntry(
    enumEntry: KtEnumEntry, support: UltraLightSupport,
    private val enumConstant: PsiEnumConstant
) : KtUltraLightClass(enumEntry, support), PsiEnumConstantInitializer {

    private val baseClassReferenceAndType: Pair<PsiJavaCodeReferenceElement, PsiClassType> by lazyPub {
        // It should not be null for not-too-complex classes and that is not the case because
        // the containing class is not too complex (since we created KtUltraLightClassForEnumEntry instance)
        val extendsList =
            super.getExtendsList() ?: error("KtUltraLightClass::getExtendsList is null for ${enumEntry.fqName}")

        Pair(
            extendsList.referenceElements.getOrNull(0) ?: error("No referenceElements found for ${enumEntry.fqName}"),
            extendsList.referencedTypes.getOrNull(0) ?: error("No referencedTypes found for ${enumEntry.fqName}")
        )
    }

    override fun getBaseClassType() = baseClassReferenceAndType.second

    override fun getBaseClassReference() = baseClassReferenceAndType.first

    override fun getArgumentList(): PsiExpressionList? = null

    override fun getEnumConstant() = enumConstant

    override fun isInQualifiedNew() = false
}
