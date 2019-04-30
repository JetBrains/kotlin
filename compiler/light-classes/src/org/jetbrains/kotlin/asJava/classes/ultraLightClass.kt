/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.google.common.annotations.VisibleForTesting
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegationResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.resolve.inline.isInlineOnly
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.STRICTFP_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

open class KtUltraLightClass(classOrObject: KtClassOrObject, internal val support: KtUltraLightSupport) :
    KtLightClassImpl(classOrObject) {
    companion object {

        // This property may be removed once IntelliJ versions earlier than 2018.3 become unsupported
        // And usages of that property may be replaced with relevant registry key
        @Volatile
        @get:TestOnly
        var forceUsingOldLightClasses = false
    }

    private val tooComplex: Boolean by lazyPub { support.isTooComplexForUltraLightGeneration(classOrObject) }

    override fun isFinal(isFinalByPsi: Boolean) = if (tooComplex) super.isFinal(isFinalByPsi) else isFinalByPsi

    @Volatile
    @VisibleForTesting
    var isClsDelegateLoaded = false

    override fun findLightClassData(): LightClassData = super.findLightClassData().also {
        if (!isClsDelegateLoaded) {
            isClsDelegateLoaded = true
            check(tooComplex) {
                "Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: $qualifiedName"
            }
        }
    }

    private fun allSuperTypes() =
        getDescriptor()?.typeConstructor?.supertypes.orEmpty().asSequence()

    private fun mapSupertype(supertype: KotlinType) =
        supertype.asPsiType(support, TypeMappingMode.SUPER_TYPE, this) as? PsiClassType

    override fun createExtendsList(): PsiReferenceList? =
        if (tooComplex) super.createExtendsList()
        else KotlinLightReferenceListBuilder(
            manager,
            language,
            PsiReferenceList.Role.EXTENDS_LIST
        ).also { list ->
            allSuperTypes()
                .filter(this::isTypeForExtendsList)
                .map(this::mapSupertype)
                .forEach(list::addReference)
        }

    private fun isTypeForExtendsList(supertype: KotlinType): Boolean {
        // Do not add redundant "extends java.lang.Object" anywhere
        if (supertype.isAnyOrNullableAny()) return false

        // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
        if (isEnum && KotlinBuiltIns.isEnum(supertype)) return false

        // Interfaces have only extends lists
        if (isInterface) return true

        return !JvmCodegenUtil.isJvmInterface(supertype)
    }

    override fun createImplementsList(): PsiReferenceList? =
        if (tooComplex) super.createImplementsList()
        else KotlinLightReferenceListBuilder(
            manager,
            language,
            PsiReferenceList.Role.IMPLEMENTS_LIST
        ).also { list ->
            if (!isInterface) {
                allSuperTypes()
                    .filter { JvmCodegenUtil.isJvmInterface(it) }
                    .map(this::mapSupertype)
                    .forEach(list::addReference)
            }
        }

    override fun buildTypeParameterList(): PsiTypeParameterList =
        if (tooComplex) super.buildTypeParameterList() else buildTypeParameterList(classOrObject, this, support)

    // the following logic should be in the platform (super), overrides can be removed once that happens
    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)

    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)
    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun getRBrace(): PsiElement? = null
    override fun getLBrace(): PsiElement? = null

    private val _ownFields: List<KtLightField> by lazyPub {
        val result = arrayListOf<KtLightField>()
        val usedNames = hashSetOf<String>()

        fun generateUniqueName(base: String): String {
            if (usedNames.add(base)) return base
            var i = 1
            while (true) {
                val suggestion = "$base$$i"
                if (usedNames.add(suggestion)) return suggestion
                i++
            }
        }


        for (parameter in propertyParameters()) {
            propertyField(parameter, ::generateUniqueName, forceStatic = false)?.let(result::add)
        }

        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            result.add(
                KtUltraLightField(
                    companion,
                    generateUniqueName(companion.name.orEmpty()),
                    this,
                    support,
                    setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                )
            )

            for (property in companion.declarations.filterIsInstance<KtProperty>()) {
                if (isInterface && !property.isConstOrJvmField()) continue
                propertyField(property, ::generateUniqueName, true)?.let(result::add)
            }
        }

        if (!isInterface) {
            val isCompanion = this.classOrObject is KtObjectDeclaration && this.classOrObject.isCompanion()
            for (property in this.classOrObject.declarations.filterIsInstance<KtProperty>()) {
                // All fields for companion object of classes are generated to the containing class
                // For interfaces, only @JvmField-annotated properties are generated to the containing class
                // Probably, the same should work for const vals but it doesn't at the moment (see KT-28294)
                if (isCompanion && (containingClass?.isInterface == false || property.isJvmField())) continue

                propertyField(property, ::generateUniqueName, forceStatic = this.classOrObject is KtObjectDeclaration)?.let(result::add)
            }
        }

        if (isNamedObject()) {
            result.add(
                KtUltraLightField(
                    this.classOrObject,
                    JvmAbi.INSTANCE_FIELD,
                    this,
                    support,
                    setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                )
            )
        }

        if (isEnum) {
            for (ktEnumEntry in classOrObject.declarations.filterIsInstance<KtEnumEntry>()) {
                val name = ktEnumEntry.name ?: continue
                result.add(
                    KtUltraLightEnumEntry(
                        ktEnumEntry, name, this, support,
                        setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                    )
                )
            }
        }

        result
    }

    private fun isNamedObject() = classOrObject is KtObjectDeclaration && !classOrObject.isCompanion()

    private fun propertyField(
        // KtProperty | KtParameter
        variable: KtCallableDeclaration,
        generateUniqueName: (String) -> String,
        forceStatic: Boolean
    ): KtLightField? {
        val property = variable as? KtProperty
        if (property != null && !hasBackingField(property)) return null

        if (variable.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)) return null

        val hasDelegate = property?.hasDelegate() == true
        val fieldName = generateUniqueName((variable.name ?: "") + (if (hasDelegate) "\$delegate" else ""))

        val visibility = when {
            variable.hasModifier(PRIVATE_KEYWORD) -> PsiModifier.PRIVATE
            variable.hasModifier(LATEINIT_KEYWORD) || variable.isConstOrJvmField() -> {
                val declaration = property?.setter ?: variable
                simpleVisibility(declaration)
            }
            else -> PsiModifier.PRIVATE
        }
        val modifiers = hashSetOf(visibility)

        val isMutable = when (variable) {
            is KtProperty -> variable.isVar
            is KtParameter -> variable.isMutable
            else -> error("Unexpected type of variable: ${variable::class.java}")
        }

        if (!isMutable || variable.hasModifier(CONST_KEYWORD) || hasDelegate) {
            modifiers.add(PsiModifier.FINAL)
        }

        if (forceStatic || isNamedObject() && isJvmStatic(variable)) {
            modifiers.add(PsiModifier.STATIC)
        }

        return KtUltraLightField(variable, fieldName, this, support, modifiers)
    }

    private fun hasBackingField(property: KtProperty): Boolean {
        if (property.hasModifier(ABSTRACT_KEYWORD)) return false
        if (property.hasModifier(LATEINIT_KEYWORD) || property.accessors.isEmpty()) return true

        val context = LightClassGenerationSupport.getInstance(project).analyze(property)
        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property)
        return descriptor is PropertyDescriptor && context[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
    }

    override fun getOwnFields(): List<KtLightField> = if (tooComplex) super.getOwnFields() else _ownFields

    private fun propertyParameters() = classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() }

    private val _ownMethods: List<KtLightMethod> by lazyPub {

        val result = arrayListOf<KtLightMethod>()

        for (declaration in this.classOrObject.declarations.filterNot { isHiddenByDeprecation(it) }) {
            if (declaration.hasModifier(PRIVATE_KEYWORD) && isInterface) continue
            when (declaration) {
                is KtNamedFunction -> result.addAll(asJavaMethods(declaration, false))
                is KtProperty -> result.addAll(propertyAccessors(declaration, declaration.isVar, false))
            }
        }

        for (parameter in propertyParameters()) {
            result.addAll(propertyAccessors(parameter, parameter.isMutable, false))
        }

        if (!isInterface) {
            result.addAll(createConstructors())
        }

        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            for (declaration in companion.declarations.filterNot { isHiddenByDeprecation(it) }) {
                when (declaration) {
                    is KtNamedFunction -> if (isJvmStatic(declaration)) result.addAll(asJavaMethods(declaration, true))
                    is KtProperty -> result.addAll(propertyAccessors(declaration, declaration.isVar, true))
                }
            }
        }

        addMethodsFromDataClass(result)
        addDelegatesToInterfaceMethods(result)

        result
    }

    private fun addMethodsFromDataClass(result: MutableList<KtLightMethod>) {
        if (!classOrObject.hasModifier(DATA_KEYWORD)) return
        val descriptor = classOrObject.resolve() as? ClassDescriptor ?: return
        val bindingContext = classOrObject.analyze()

        // Force resolving data class members set
        descriptor.unsubstitutedMemberScope.getContributedDescriptors()

        object : DataClassMethodGenerator(classOrObject, bindingContext) {
            private fun addFunction(descriptor: FunctionDescriptor, declarationForOrigin: KtDeclaration? = null) {
                result.add(createGeneratedMethodFromDescriptor(descriptor, declarationForOrigin))
            }

            override fun generateComponentFunction(function: FunctionDescriptor, parameter: ValueParameterDescriptor) {
                addFunction(function, DescriptorToSourceUtils.descriptorToDeclaration(parameter) as? KtDeclaration)
            }

            override fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>) {
                addFunction(function)
            }

            override fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
                addFunction(function)
            }

            override fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
                addFunction(function)
            }

            override fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
                addFunction(function)
            }
        }.generate()
    }

    private fun addDelegatesToInterfaceMethods(result: MutableList<KtLightMethod>) {
        classOrObject.superTypeListEntries.filterIsInstance<KtDelegatedSuperTypeEntry>().forEach {
            addDelegatesToInterfaceMethods(it, result)
        }
    }

    private fun addDelegatesToInterfaceMethods(
        superTypeEntry: KtDelegatedSuperTypeEntry,
        result: MutableList<KtLightMethod>
    ) {
        val classDescriptor = classOrObject.resolve() as? ClassDescriptor ?: return
        val typeReference = superTypeEntry.typeReference ?: return
        val bindingContext = typeReference.analyze()

        val superClassDescriptor = CodegenUtil.getSuperClassBySuperTypeListEntry(superTypeEntry, bindingContext) ?: return
        val delegationType = superTypeEntry.delegateExpression.kotlinType(bindingContext) ?: return

        for (delegate in DelegationResolver.getDelegates(classDescriptor, superClassDescriptor, delegationType).keys) {
            when (delegate) {

                is PropertyDescriptor -> delegate.accessors.mapTo(result) {
                    createGeneratedMethodFromDescriptor(it)
                }
                is FunctionDescriptor -> result.add(createGeneratedMethodFromDescriptor(delegate))
            }
        }
    }

    private fun createConstructors(): List<KtLightMethod> {
        val result = arrayListOf<KtLightMethod>()
        val constructors = classOrObject.allConstructors
        if (constructors.isEmpty()) {
            result.add(defaultConstructor())
        }
        for (constructor in constructors.filterNot { isHiddenByDeprecation(it) }) {
            result.addAll(asJavaMethods(constructor, false, forcePrivate = isEnum))
        }
        val primary = classOrObject.primaryConstructor
        if (primary != null && shouldGenerateNoArgOverload(primary)) {
            result.add(noArgConstructor(simpleVisibility(primary), primary))
        }
        return result
    }

    private fun shouldGenerateNoArgOverload(primary: KtPrimaryConstructor): Boolean {
        return !primary.hasModifier(PRIVATE_KEYWORD) &&
                !classOrObject.hasModifier(INNER_KEYWORD) && !isEnum &&
                !classOrObject.hasModifier(SEALED_KEYWORD) &&
                primary.valueParameters.isNotEmpty() &&
                primary.valueParameters.all { it.defaultValue != null } &&
                classOrObject.allConstructors.none { it.valueParameters.isEmpty() } &&
                !primary.hasAnnotation(JVM_OVERLOADS_FQ_NAME)
    }

    private fun defaultConstructor(): KtUltraLightMethod {
        val visibility =
            when {
                classOrObject is KtObjectDeclaration || classOrObject.hasModifier(SEALED_KEYWORD) || isEnum -> PsiModifier.PRIVATE
                classOrObject is KtEnumEntry -> PsiModifier.PACKAGE_LOCAL
                else -> PsiModifier.PUBLIC
            }
        return noArgConstructor(visibility, classOrObject)
    }

    private fun simpleVisibility(declaration: KtDeclaration): String = when {
        declaration.hasModifier(PRIVATE_KEYWORD) -> PsiModifier.PRIVATE
        declaration.hasModifier(PROTECTED_KEYWORD) -> PsiModifier.PROTECTED
        else -> PsiModifier.PUBLIC
    }

    private fun noArgConstructor(visibility: String, declaration: KtDeclaration): KtUltraLightMethod =
        KtUltraLightMethodForSourceDeclaration(
            LightMethodBuilder(manager, language, name.orEmpty()).setConstructor(true).addModifier(visibility),
            declaration,
            support,
            this
        )

    private fun isHiddenByDeprecation(declaration: KtDeclaration): Boolean {
        val deprecated = support.findAnnotation(declaration, FqName("kotlin.Deprecated"))?.second
        return (deprecated?.argumentValue("level") as? EnumValue)?.enumEntryName?.asString() == "HIDDEN"
    }

    private fun isJvmStatic(declaration: KtAnnotated): Boolean = declaration.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME)

    override fun getOwnMethods(): List<KtLightMethod> = if (tooComplex) super.getOwnMethods() else _ownMethods

    private fun asJavaMethods(
        ktFunction: KtFunction,
        forceStatic: Boolean,
        forcePrivate: Boolean = false
    ): Collection<KtLightMethod> {

        if (ktFunction.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)) return emptyList()

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

    private fun asJavaMethod(
        ktFunction: KtFunction,
        forceStatic: Boolean,
        forcePrivate: Boolean,
        numberOfDefaultParametersToAdd: Int = -1
    ): KtLightMethod {
        val isConstructor = ktFunction is KtConstructor<*>
        val name =
            if (isConstructor)
                this.name
            else computeMethodName(ktFunction, ktFunction.name ?: SpecialNames.NO_NAME_PROVIDED.asString(), MethodType.REGULAR)

        val method = lightMethod(name.orEmpty(), ktFunction, forceStatic, forcePrivate)
        val wrapper = KtUltraLightMethodForSourceDeclaration(method, ktFunction, support, this)
        addReceiverParameter(ktFunction, wrapper)


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
        val returnType: PsiType? by lazyPub {
            if (isConstructor) null
            else methodReturnType(ktFunction, wrapper)
        }
        method.setMethodReturnType { returnType }
        return wrapper
    }

    private fun addReceiverParameter(callable: KtCallableDeclaration, method: KtUltraLightMethod) {
        if (callable.receiverTypeReference == null) return
        method.delegate.addParameter(KtUltraLightReceiverParameter(callable, support, method))
    }

    private fun methodReturnType(ktDeclaration: KtDeclaration, wrapper: KtUltraLightMethod): PsiType {
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
        return LightMethodBuilder(
            manager, language, name,
            LightParameterListBuilder(manager, language),
            object : LightModifierList(manager, language) {
                override fun hasModifierProperty(name: String): Boolean {
                    if (name == PsiModifier.PUBLIC || name == PsiModifier.PROTECTED || name == PsiModifier.PRIVATE) {
                        if (forcePrivate || declaration.isPrivate() || accessedProperty?.isPrivate() == true) {
                            return name == PsiModifier.PRIVATE
                        }
                        if (declaration.hasModifier(PROTECTED_KEYWORD) || accessedProperty?.hasModifier(PROTECTED_KEYWORD) == true) {
                            return name == PsiModifier.PROTECTED
                        }

                        if (outer.hasModifier(OVERRIDE_KEYWORD)) {
                            when ((outer.resolve() as? CallableDescriptor)?.visibility) {
                                Visibilities.PUBLIC -> return name == PsiModifier.PUBLIC
                                Visibilities.PRIVATE -> return name == PsiModifier.PRIVATE
                                Visibilities.PROTECTED -> return name == PsiModifier.PROTECTED
                            }
                        }

                        return name == PsiModifier.PUBLIC
                    }

                    return when (name) {
                        PsiModifier.FINAL -> !isInterface && outer !is KtConstructor<*> && isFinal(outer)
                        PsiModifier.ABSTRACT -> isInterface || outer.hasModifier(ABSTRACT_KEYWORD)
                        PsiModifier.STATIC -> forceStatic || isNamedObject() && (isJvmStatic(outer) || isJvmStatic(declaration))
                        PsiModifier.STRICTFP -> declaration is KtFunction && declaration.hasAnnotation(STRICTFP_ANNOTATION_FQ_NAME)
                        PsiModifier.SYNCHRONIZED -> declaration is KtFunction && declaration.hasAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME)
                        else -> false
                    }
                }

                fun KtDeclaration.isPrivate() =
                    hasModifier(PRIVATE_KEYWORD) ||
                            this is KtConstructor<*> && classOrObject.hasModifier(SEALED_KEYWORD) ||
                            isInlineOnly()

                private fun KtDeclaration.isInlineOnly(): Boolean {
                    if (this !is KtCallableDeclaration || !hasModifier(INLINE_KEYWORD)) return false
                    if (typeParameters.any { it.hasModifier(REIFIED_KEYWORD) }) return true
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

        if (isInternalNonPublishedApi(declaration)) return KotlinTypeMapper.InternalNameMapper.mangleInternalName(name, support.moduleName)
        return name
    }

    private tailrec fun isInternalNonPublishedApi(declaration: KtDeclaration): Boolean {
        if (declaration.hasModifier(PRIVATE_KEYWORD) ||
            declaration.hasModifier(PROTECTED_KEYWORD) ||
            declaration.hasModifier(PUBLIC_KEYWORD)
        ) {
            return false
        }

        if (isInternal(declaration) && declaration.resolve()?.isPublishedApi() != true) return true

        val containingProperty = (declaration as? KtPropertyAccessor)?.property ?: return false
        return isInternalNonPublishedApi(containingProperty)
    }

    private fun KtAnnotated.hasAnnotation(name: FqName) = support.findAnnotation(this, name) != null

    private fun isInternal(f: KtDeclaration): Boolean {
        if (f.hasModifier(OVERRIDE_KEYWORD)) {
            val desc = f.resolve()
            return desc is CallableDescriptor &&
                    desc.visibility.effectiveVisibility(desc, false) == EffectiveVisibility.Internal
        }
        return f.hasModifier(INTERNAL_KEYWORD)
    }

    private fun propertyAccessors(
        declaration: KtCallableDeclaration,
        mutable: Boolean,
        onlyJvmStatic: Boolean
    ): List<KtLightMethod> {

        val propertyName = declaration.name ?: return emptyList()
        if (declaration.isConstOrJvmField()) return emptyList()

        val ktGetter = (declaration as? KtProperty)?.getter
        val ktSetter = (declaration as? KtProperty)?.setter

        val isPrivate = declaration.hasModifier(PRIVATE_KEYWORD)
        if (isPrivate && declaration !is KtProperty) return emptyList()

        fun needsAccessor(accessor: KtPropertyAccessor?): Boolean {
            if (!onlyJvmStatic || isJvmStatic(declaration) || accessor != null && isJvmStatic(accessor)) {
                if (declaration is KtProperty && declaration.hasDelegate()) {
                    return true
                }
                if (accessor?.hasModifier(PRIVATE_KEYWORD) == true || accessor?.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) == true) {
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
            val getterName = computeMethodName(ktGetter ?: declaration, JvmAbi.getterName(propertyName), MethodType.GETTER)
            val getterPrototype = lightMethod(getterName, ktGetter ?: declaration, onlyJvmStatic)
            val getterWrapper = KtUltraLightMethodForSourceDeclaration(getterPrototype, declaration, support, this)
            val getterType: PsiType by lazyPub { methodReturnType(declaration, getterWrapper) }
            getterPrototype.setMethodReturnType { getterType }
            addReceiverParameter(declaration, getterWrapper)
            result.add(getterWrapper)
        }

        if (mutable && needsAccessor(ktSetter)) {
            val setterName = computeMethodName(ktSetter ?: declaration, JvmAbi.setterName(propertyName), MethodType.SETTER)
            val setterPrototype = lightMethod(setterName, ktSetter ?: declaration, onlyJvmStatic)
                .setMethodReturnType(PsiType.VOID)
            val setterWrapper = KtUltraLightMethodForSourceDeclaration(setterPrototype, declaration, support, this)
            addReceiverParameter(declaration, setterWrapper)
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

    private fun KtCallableDeclaration.isConstOrJvmField() =
        hasModifier(CONST_KEYWORD) || isJvmField()

    private fun KtCallableDeclaration.isJvmField() = hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)

    private fun isFinal(declaration: KtDeclaration): Boolean {
        if (declaration.hasModifier(FINAL_KEYWORD)) return true
        return declaration !is KtPropertyAccessor &&
                !declaration.hasModifier(OPEN_KEYWORD) &&
                !declaration.hasModifier(OVERRIDE_KEYWORD) &&
                !declaration.hasModifier(ABSTRACT_KEYWORD)
    }

    override fun getInitializers(): Array<PsiClassInitializer> = emptyArray()

    override fun getContainingClass(): PsiClass? =
        if (tooComplex) super.getContainingClass()
        else ((classOrObject.parent as? KtClassBody)?.parent as? KtClassOrObject)?.let(KtLightClassForSourceDeclaration::create)

    override fun getParent(): PsiElement? = if (tooComplex) super.getParent() else containingClass ?: containingFile

    override fun getScope(): PsiElement? = if (tooComplex) super.getScope() else parent
    override fun copy(): KtLightClassImpl = KtUltraLightClass(classOrObject.copy() as KtClassOrObject, support)
}
