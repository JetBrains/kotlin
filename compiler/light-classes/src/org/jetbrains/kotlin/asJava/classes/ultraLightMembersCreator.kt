/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.convertToLightAnnotationMemberValue
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.REIFIED_KEYWORD
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.resolve.inline.isInlineOnly
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.STRICTFP_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

internal class UltraLightMembersCreator(
    private val containingClass: KtLightClass,
    private val containingClassIsNamedObject: Boolean,
    private val containingClassIsSealed: Boolean,
    private val mangleInternalFunctions: Boolean,
    private val support: KtUltraLightSupport
) {

    fun generateUniqueFieldName(base: String, usedNames: HashSet<String>): String {
        if (usedNames.add(base)) return base
        var i = 1
        while (true) {
            val suggestion = "$base$$i"
            if (usedNames.add(suggestion)) return suggestion
            i++
        }
    }

    fun createPropertyField(
        // KtProperty | KtParameter
        variable: KtCallableDeclaration,
        usedPropertyNames: HashSet<String>,
        forceStatic: Boolean
    ): KtLightField? {
        val property = variable as? KtProperty
        if (property != null && !hasBackingField(property)) return null

        if (variable.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) || variable.hasExpectModifier()) return null

        val hasDelegate = property?.hasDelegate() == true
        val fieldName = generateUniqueFieldName((variable.name ?: "") + (if (hasDelegate) "\$delegate" else ""), usedPropertyNames)

        val visibility = when {
            variable.hasModifier(KtTokens.PRIVATE_KEYWORD) -> PsiModifier.PRIVATE
            variable.hasModifier(KtTokens.LATEINIT_KEYWORD) || variable.isConstOrJvmField() -> {
                val declaration = property?.setter ?: variable
                declaration.simpleVisibility()
            }
            else -> PsiModifier.PRIVATE
        }
        val modifiers = hashSetOf(visibility)

        val isMutable = when (variable) {
            is KtProperty -> variable.isVar
            is KtParameter -> variable.isMutable
            else -> error("Unexpected type of variable: ${variable::class.java}")
        }

        if (!isMutable || variable.hasModifier(KtTokens.CONST_KEYWORD) || hasDelegate) {
            modifiers.add(PsiModifier.FINAL)
        }

        if (forceStatic || containingClassIsNamedObject && variable.isJvmStatic(support)) {
            modifiers.add(PsiModifier.STATIC)
        }

        return KtUltraLightFieldForSourceDeclaration(variable, fieldName, containingClass, support, modifiers)
    }

    private fun hasBackingField(property: KtProperty): Boolean {
        if (property.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return false
        if (property.hasModifier(KtTokens.LATEINIT_KEYWORD) || property.accessors.isEmpty()) return true

        val context = LightClassGenerationSupport.getInstance(containingClass.project).analyze(property)
        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property)
        return descriptor is PropertyDescriptor && context[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
    }

    fun createMethods(
        ktFunction: KtFunction,
        forceStatic: Boolean,
        forcePrivate: Boolean = false
    ): Collection<KtLightMethod> {

        if (ktFunction.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) ||
            ktFunction.hasReifiedParameters() ||
            ktFunction.hasExpectModifier()
        ) return emptyList()

        val basicMethod = asJavaMethod(ktFunction, forceStatic, forcePrivate)

        val result = mutableListOf(basicMethod)

        if (ktFunction.hasAnnotation(JVM_OVERLOADS_FQ_NAME)) {
            val numberOfDefaultParameters = ktFunction.valueParameters.count(KtParameter::hasDefaultValue)
            for (numberOfDefaultParametersToAdd in numberOfDefaultParameters - 1 downTo 0) {
                result.add(
                    asJavaMethod(
                        ktFunction,
                        forceStatic,
                        forcePrivate,
                        numberOfDefaultParametersToAdd = numberOfDefaultParametersToAdd
                    )
                )
            }
        }

        return result
    }

    internal class KtUltraLightAnnotationMethod(
        psiMethod: KtLightMethod,
        expression: KtExpression
    ) : KtLightMethod by psiMethod,
        PsiAnnotationMethod {

        private val value by lazyPub {
            convertToLightAnnotationMemberValue(psiMethod, expression)
        }

        override fun getDefaultValue(): PsiAnnotationMemberValue? = value
    }

    private fun asJavaMethod(
        ktFunction: KtFunction,
        forceStatic: Boolean,
        forcePrivate: Boolean,
        numberOfDefaultParametersToAdd: Int = -1
    ): KtLightMethod {
        val isConstructor = ktFunction is KtConstructor<*>
        val name =
            if (isConstructor)
                containingClass.name
            else computeMethodName(ktFunction, ktFunction.name ?: SpecialNames.NO_NAME_PROVIDED.asString(), MethodType.REGULAR)

        val method = lightMethod(name.orEmpty(), ktFunction, forceStatic, forcePrivate)
        val wrapper = KtUltraLightMethodForSourceDeclaration(method, ktFunction, support, containingClass)
        addReceiverParameter(ktFunction, wrapper, method)

        var remainingNumberOfDefaultParametersToAdd =
            if (numberOfDefaultParametersToAdd >= 0)
                numberOfDefaultParametersToAdd
            else
            // Just to avoid computing the actual number of default parameters, we use an upper bound
                ktFunction.valueParameters.size

        for (parameter in ktFunction.valueParameters) {
            if (parameter.hasDefaultValue()) {
                if (remainingNumberOfDefaultParametersToAdd == 0) continue
                remainingNumberOfDefaultParametersToAdd--
            }

            method.addParameter(KtUltraLightParameterForSource(parameter.name.orEmpty(), parameter, support, wrapper, ktFunction))
        }

        val isSuspendFunction = ktFunction.modifierList?.hasSuspendModifier() == true
        if (isSuspendFunction) {
            method.addParameter(KtUltraLightSuspendContinuationParameter(ktFunction, support, wrapper))
        }

        val returnType: PsiType? by lazyPub {
            when {
                isConstructor -> null
                else -> methodReturnType(ktFunction, wrapper, isSuspendFunction)
            }
        }

        method.setMethodReturnType { returnType }
        return wrapper
    }

    private fun addReceiverParameter(callable: KtCallableDeclaration, wrapper: KtUltraLightMethod, associatedBuilder: LightMethodBuilder) {
        if (callable.receiverTypeReference == null) return

        require(wrapper.delegate == associatedBuilder) {
            "Invalid use. Wrapper does not wrap an associated method builder."
        }

        associatedBuilder.addParameter(KtUltraLightReceiverParameter(callable, support, wrapper))
    }

    private fun methodReturnType(ktDeclaration: KtDeclaration, wrapper: KtUltraLightMethod, isSuspendFunction: Boolean): PsiType {

        if (isSuspendFunction) {
            return support.moduleDescriptor
                .builtIns
                .nullableAnyType
                .asPsiType(support, TypeMappingMode.DEFAULT, wrapper)
        }

        if (ktDeclaration is KtNamedFunction &&
            ktDeclaration.hasBlockBody() &&
            !ktDeclaration.hasDeclaredReturnType()
        ) return PsiType.VOID

        val desc =
            ktDeclaration.resolve()?.getterIfProperty() as? FunctionDescriptor
                ?: return PsiType.NULL

        return support.mapType(wrapper) { typeMapper, signatureWriter ->
            typeMapper.mapReturnType(desc, signatureWriter)
        }
    }

    private fun DeclarationDescriptor.getterIfProperty() =
        if (this@getterIfProperty is PropertyDescriptor) this@getterIfProperty.getter else this@getterIfProperty

    private fun lightMethod(
        name: String,
        declaration: KtDeclaration,
        forceStatic: Boolean,
        forcePrivate: Boolean = false
    ): LightMethodBuilder {
        val accessedProperty = if (declaration is KtPropertyAccessor) declaration.property else null
        val outer = accessedProperty ?: declaration

        val manager = declaration.manager
        val language = declaration.language

        return LightMethodBuilder(
            manager, language, name,
            LightParameterListBuilder(manager, language),
            object : LightModifierList(manager, language) {
                override fun hasModifierProperty(name: String): Boolean {
                    if (name == PsiModifier.PUBLIC || name == PsiModifier.PROTECTED || name == PsiModifier.PRIVATE) {
                        if (forcePrivate || declaration.isPrivate() || accessedProperty?.isPrivate() == true) {
                            return name == PsiModifier.PRIVATE
                        }
                        if (declaration.hasModifier(KtTokens.PROTECTED_KEYWORD) || accessedProperty?.hasModifier(KtTokens.PROTECTED_KEYWORD) == true) {
                            return name == PsiModifier.PROTECTED
                        }

                        if (outer.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
                            when ((outer.resolve() as? CallableDescriptor)?.visibility) {
                                Visibilities.PUBLIC -> return name == PsiModifier.PUBLIC
                                Visibilities.PRIVATE -> return name == PsiModifier.PRIVATE
                                Visibilities.PROTECTED -> return name == PsiModifier.PROTECTED
                            }
                        }

                        return name == PsiModifier.PUBLIC
                    }

                    return when (name) {
                        PsiModifier.FINAL -> !containingClass.isInterface && outer !is KtConstructor<*> && isFinal(outer)
                        PsiModifier.ABSTRACT -> containingClass.isInterface || outer.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                        PsiModifier.STATIC -> forceStatic || containingClassIsNamedObject && (outer.isJvmStatic(support) || declaration.isJvmStatic(
                            support
                        ))
                        PsiModifier.STRICTFP -> declaration is KtFunction && declaration.hasAnnotation(STRICTFP_ANNOTATION_FQ_NAME)
                        PsiModifier.SYNCHRONIZED -> declaration is KtFunction && declaration.hasAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME)
                        else -> false
                    }
                }

                fun KtDeclaration.isPrivate() =
                    hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                            this is KtConstructor<*> && containingClassIsSealed || isInlineOnly()

                private fun KtDeclaration.isInlineOnly(): Boolean {
                    if (this !is KtCallableDeclaration || !hasModifier(KtTokens.INLINE_KEYWORD)) return false
                    if (annotationEntries.isEmpty()) return false

                    val descriptor = resolve() as? CallableMemberDescriptor ?: return false

                    return descriptor.isInlineOnly()
                }
            }
        ).setConstructor(declaration is KtConstructor<*>)
    }

    private enum class MethodType {
        REGULAR,
        GETTER,
        SETTER
    }

    private fun computeMethodName(declaration: KtDeclaration, name: String, type: MethodType): String {

        fun tryCompute(declaration: KtDeclaration, type: MethodType): String? {

            if (!declaration.hasAnnotation(DescriptorUtils.JVM_NAME)) return null

            val annotated = (declaration.resolve() as? Annotated) ?: return null

            val resultName = DescriptorUtils.getJvmName(annotated)
            if (resultName !== null || type == MethodType.REGULAR) return resultName

            val propertyAnnotated = when (type) {
                MethodType.GETTER -> (annotated as? PropertyDescriptor)?.getter
                MethodType.SETTER -> (annotated as? PropertyDescriptor)?.setter
                else -> throw NotImplementedError()
            }

            return propertyAnnotated?.let(DescriptorUtils::getJvmName)
        }

        val computedName = tryCompute(declaration, type)
        if (computedName !== null) return computedName

        return if (mangleInternalFunctions && isInternalNonPublishedApi(declaration))
            KotlinTypeMapper.InternalNameMapper.mangleInternalName(name, support.moduleName)
        else name
    }

    private tailrec fun isInternalNonPublishedApi(declaration: KtDeclaration): Boolean {
        if (declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
            declaration.hasModifier(KtTokens.PROTECTED_KEYWORD) ||
            declaration.hasModifier(KtTokens.PUBLIC_KEYWORD)
        ) {
            return false
        }

        if (isInternal(declaration) && declaration.resolve()?.isPublishedApi() != true) return true

        val containingProperty = (declaration as? KtPropertyAccessor)?.property ?: return false
        return isInternalNonPublishedApi(containingProperty)
    }

    private fun KtAnnotated.hasAnnotation(name: FqName) = support.findAnnotation(this, name) != null

    private fun isInternal(f: KtDeclaration): Boolean {
        if (f.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            val desc = f.resolve()
            return desc is CallableDescriptor &&
                    desc.visibility.effectiveVisibility(desc, false) == EffectiveVisibility.Internal
        }
        return f.hasModifier(KtTokens.INTERNAL_KEYWORD)
    }

    fun propertyAccessors(
        declaration: KtCallableDeclaration,
        mutable: Boolean,
        forceStatic: Boolean,
        onlyJvmStatic: Boolean,
        createAsAnnotationMethod: Boolean = false
    ): List<KtLightMethod> {

        val propertyName = declaration.name ?: return emptyList()
        if (declaration.isConstOrJvmField() ||
            declaration.hasReifiedParameters() ||
            declaration.hasExpectModifier()
        ) return emptyList()

        val ktGetter = (declaration as? KtProperty)?.getter
        val ktSetter = (declaration as? KtProperty)?.setter

        val isPrivate = !forceStatic && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)
        if (isPrivate && declaration !is KtProperty) return emptyList()

        fun needsAccessor(accessor: KtPropertyAccessor?): Boolean {
            if (!onlyJvmStatic || declaration.isJvmStatic(support) || accessor != null && accessor.isJvmStatic(support)) {
                if (declaration is KtProperty && declaration.hasDelegate()) {
                    return true
                }
                if (accessor?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true || accessor?.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) == true) {
                    return false
                }
                if (!isPrivate || accessor?.hasBody() == true) {
                    return true
                }
            }
            return false
        }

        val result = arrayListOf<KtLightMethod>()

        if (needsAccessor(ktGetter)) {
            val auxiliaryOrigin = ktGetter ?: declaration
            val lightMemberOrigin = LightMemberOriginForDeclaration(
                originalElement = declaration,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = auxiliaryOrigin
            )

            val defaultGetterName = if (createAsAnnotationMethod) propertyName else JvmAbi.getterName(propertyName)
            val getterName = computeMethodName(auxiliaryOrigin, defaultGetterName, MethodType.GETTER)
            val getterPrototype = lightMethod(getterName, auxiliaryOrigin, forceStatic = onlyJvmStatic || forceStatic)
            val getterWrapper = KtUltraLightMethodForSourceDeclaration(
                getterPrototype,
                lightMemberOrigin,
                support,
                containingClass,
                forceToSkipNullabilityAnnotation = createAsAnnotationMethod
            )

            val getterType: PsiType by lazyPub { methodReturnType(declaration, getterWrapper, isSuspendFunction = false) }
            getterPrototype.setMethodReturnType { getterType }
            addReceiverParameter(declaration, getterWrapper, getterPrototype)

            val defaultExpression = if (createAsAnnotationMethod && declaration is KtParameter) declaration.defaultValue else null
            val getterMethodResult = defaultExpression?.let {
                KtUltraLightAnnotationMethod(getterWrapper, it)
            } ?: getterWrapper

            result.add(getterMethodResult)
        }

        if (!createAsAnnotationMethod && mutable && needsAccessor(ktSetter)) {
            val auxiliaryOrigin = ktSetter ?: declaration
            val lightMemberOrigin = LightMemberOriginForDeclaration(
                originalElement = declaration,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = auxiliaryOrigin
            )

            val setterName = computeMethodName(auxiliaryOrigin, JvmAbi.setterName(propertyName), MethodType.SETTER)
            val setterPrototype = lightMethod(setterName, auxiliaryOrigin, forceStatic = onlyJvmStatic || forceStatic)
                .setMethodReturnType(PsiType.VOID)

            val setterWrapper = KtUltraLightMethodForSourceDeclaration(setterPrototype, lightMemberOrigin, support, containingClass)
            addReceiverParameter(declaration, setterWrapper, setterPrototype)
            val setterParameter = ktSetter?.parameter
            setterPrototype.addParameter(
                if (setterParameter != null)
                    KtUltraLightParameterForSource(propertyName, setterParameter, support, setterWrapper, declaration)
                else
                    KtUltraLightParameterForSetterParameter(propertyName, declaration, support, setterWrapper, declaration)
            )
            result.add(setterWrapper)
        }
        return result
    }

    private fun KtCallableDeclaration.hasReifiedParameters(): Boolean =
        typeParameters.any { it.hasModifier(REIFIED_KEYWORD) }

    private fun KtCallableDeclaration.isConstOrJvmField() =
        hasModifier(KtTokens.CONST_KEYWORD) || isJvmField()

    private fun KtCallableDeclaration.isJvmField() = hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)

    private fun isFinal(declaration: KtDeclaration): Boolean {
        if (declaration.hasModifier(KtTokens.FINAL_KEYWORD)) return true
        return declaration !is KtPropertyAccessor &&
                !declaration.hasModifier(KtTokens.OPEN_KEYWORD) &&
                !declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD) &&
                !declaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)
    }
}
