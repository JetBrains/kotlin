/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.google.common.annotations.VisibleForTesting
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.asJava.UltraLightClassCodegenSupport
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtUltraLightModifierList
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DelegationResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

open class KtUltraLightClass(classOrObject: KtClassOrObject, internal val support: KtUltraLightSupport) :
    KtLightClassImpl(classOrObject) {

    private class KtUltraLightClassModifierList(
        private val containingClass: KtLightClassForSourceDeclaration,
        private val support: KtUltraLightSupport,
        private val computeModifiers: () -> Set<String>
    ) :
        KtUltraLightModifierList<KtLightClassForSourceDeclaration>(containingClass, support) {
        private val modifiers by lazyPub { computeModifiers() }

        override fun hasModifierProperty(name: String): Boolean =
            if (name != PsiModifier.FINAL) name in modifiers else owner.isFinal(PsiModifier.FINAL in modifiers)

        override fun copy(): PsiElement = KtUltraLightClassModifierList(containingClass, support, computeModifiers)
    }


    private val membersBuilder by lazyPub {
        UltraLightMembersCreator(
            this,
            isNamedObject(),
            classOrObject.hasModifier(SEALED_KEYWORD),
            mangleInternalFunctions = true,
            support = support
        )
    }

    protected val tooComplex: Boolean by lazyPub { support.isTooComplexForUltraLightGeneration(classOrObject) }

    private val _deprecated by lazyPub { classOrObject.isDeprecated(support) }

    override fun isFinal(isFinalByPsi: Boolean) = if (tooComplex) super.isFinal(isFinalByPsi) else isFinalByPsi

    @Volatile
    @VisibleForTesting
    var isClsDelegateLoaded = false

    private inline fun <T> forTooComplex(getter: () -> T): T {
        if (!isClsDelegateLoaded) {
            isClsDelegateLoaded = true
            check(tooComplex) {
                "Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: $qualifiedName"
            }
        }
        return getter()
    }

    override fun findLightClassData(): LightClassData = forTooComplex { super.findLightClassData() }

    override fun getDelegate(): PsiClass = forTooComplex { super.getDelegate() }

    private val _modifierList: PsiModifierList? by lazyPub {
        if (tooComplex) super.getModifierList() else KtUltraLightClassModifierList(this, support) { computeModifiers() }
    }

    override fun getModifierList(): PsiModifierList? = _modifierList

    private fun allSuperTypes() =
        getDescriptor()?.typeConstructor?.supertypes.orEmpty()

    private fun mapSupertype(supertype: KotlinType, kotlinCollectionAsIs: Boolean = false) =
        supertype.asPsiType(
            support,
            if (kotlinCollectionAsIs) TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS else TypeMappingMode.SUPER_TYPE,
            this
        ) as? PsiClassType

    override fun createExtendsList(): PsiReferenceList? = createInheritanceList(forExtendsList = true)

    override fun createImplementsList(): PsiReferenceList? = createInheritanceList(forExtendsList = false)

    private fun createInheritanceList(forExtendsList: Boolean): PsiReferenceList? {

        val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

        if (isAnnotationType) return KotlinLightReferenceListBuilder(manager, language, role)

        if (tooComplex) return if (forExtendsList) super.createExtendsList() else super.createImplementsList()

        val superTypes = allSuperTypes().filter {
            isTypeForInheritanceList(it, forExtendsList)
        }

        val listBuilder = KotlinSuperTypeListBuilder(
            kotlinOrigin = kotlinOrigin.getSuperTypeList(),
            manager = manager,
            language = language,
            role = role
        )

        for (superType in superTypes) {
            addTypeToTypeList(
                listBuilder = listBuilder,
                superType = superType
            )
        }

        return listBuilder
    }

    private fun addTypeToTypeList(listBuilder: KotlinSuperTypeListBuilder, superType: KotlinType) {

        val mappedType = mapSupertype(superType, kotlinCollectionAsIs = true) ?: return

        listBuilder.addReference(mappedType)

        if (mappedType.canonicalText.startsWith("kotlin.collections.")) {

            val mappedToNoCollectionAsIs = mapSupertype(superType, kotlinCollectionAsIs = false)

            if (mappedToNoCollectionAsIs !== null &&
                mappedType.canonicalText != mappedToNoCollectionAsIs.canonicalText
            ) {
                //Add java supertype
                listBuilder.addReference(mappedToNoCollectionAsIs)
                //Add marker interface
                superType.tryResolveMarkerInterfaceFQName()?.let { marker ->
                    listBuilder.addReference(marker)
                }
            }
        }
    }

    private fun isTypeForInheritanceList(supertype: KotlinType, forExtendsList: Boolean): Boolean {
        // Do not add redundant "extends java.lang.Object" anywhere
        if (supertype.isAnyOrNullableAny()) return false

        // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
        if (isEnum && KotlinBuiltIns.isEnum(supertype)) return false

        // Interfaces have only extends lists
        if (isInterface) return forExtendsList

        return forExtendsList == !JvmCodegenUtil.isJvmInterface(supertype)
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

        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            result.add(
                KtUltraLightFieldForSourceDeclaration(
                    companion,
                    membersBuilder.generateUniqueFieldName(companion.name.orEmpty(), usedNames),
                    this,
                    support,
                    setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                )
            )

            for (property in companion.declarations.filterIsInstance<KtProperty>()) {
                if (isInterface && !property.isConstOrJvmField()) continue
                membersBuilder.createPropertyField(property, usedNames, true)?.let(result::add)
            }
        }

        if (isAnnotationType) return@lazyPub result

        for (parameter in propertyParameters()) {
            membersBuilder.createPropertyField(parameter, usedNames, forceStatic = false)?.let(result::add)
        }

        if (!isInterface) {
            val isCompanion = this.classOrObject is KtObjectDeclaration && this.classOrObject.isCompanion()
            for (property in this.classOrObject.declarations.filterIsInstance<KtProperty>()) {
                // All fields for companion object of classes are generated to the containing class
                // For interfaces, only @JvmField-annotated properties are generated to the containing class
                // Probably, the same should work for const vals but it doesn't at the moment (see KT-28294)
                if (isCompanion && (containingClass?.isInterface == false || property.isJvmField())) continue

                membersBuilder.createPropertyField(property, usedNames, forceStatic = this.classOrObject is KtObjectDeclaration)
                    ?.let(result::add)
            }
        }

        if (isNamedObject()) {
            result.add(
                KtUltraLightFieldForSourceDeclaration(
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

    override fun getOwnFields(): List<KtLightField> = if (tooComplex) super.getOwnFields() else _ownFields

    private fun propertyParameters() = classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() }

    private fun ownMethods(): List<KtLightMethod> {
        val result = mutableListOf<KtLightMethod>()

        for (declaration in this.classOrObject.declarations.filterNot { it.isHiddenByDeprecation(support) }) {
            if (declaration.hasModifier(PRIVATE_KEYWORD) && isInterface) continue
            when (declaration) {
                is KtNamedFunction -> result.addAll(membersBuilder.createMethods(declaration, forceStatic = false))
                is KtProperty -> result.addAll(
                    membersBuilder.propertyAccessors(declaration, declaration.isVar, forceStatic = false, onlyJvmStatic = false)
                )
            }
        }

        for (parameter in propertyParameters()) {
            result.addAll(
                membersBuilder.propertyAccessors(
                    parameter,
                    parameter.isMutable,
                    forceStatic = false,
                    onlyJvmStatic = false,
                    createAsAnnotationMethod = isAnnotationType
                )
            )
        }

        if (!isInterface) {
            result.addAll(createConstructors())
        }

        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            for (declaration in companion.declarations.filterNot { isHiddenByDeprecation(it) }) {
                when (declaration) {
                    is KtNamedFunction ->
                        if (isJvmStatic(declaration)) result.addAll(membersBuilder.createMethods(declaration, forceStatic = true))
                    is KtProperty -> result.addAll(
                        membersBuilder.propertyAccessors(
                            declaration,
                            declaration.isVar,
                            forceStatic = false,
                            onlyJvmStatic = true
                        )
                    )
                }
            }
        }

        addMethodsFromDataClass(result)
        addDelegatesToInterfaceMethods(result)

        val lazyDescriptor = lazy { getDescriptor() }

        ExpressionCodegenExtension.getInstances(project).forEach {
            if (it is UltraLightClassCodegenSupport) {
                it.interceptMethodsBuilding(kotlinOrigin, lazyDescriptor, this, result)
            }
        }

        return result
    }

    private val _ownMethods: CachedValue<List<KtLightMethod>> = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result.create(
                ownMethods(),
                classOrObject.getExternalDependencies()
            )
        }, false
    )

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
        val delegationType = superTypeEntry.delegateExpression.kotlinType(bindingContext)

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
            result.addAll(membersBuilder.createMethods(constructor, false, forcePrivate = isEnum))
        }
        val primary = classOrObject.primaryConstructor
        if (primary != null && shouldGenerateNoArgOverload(primary)) {
            result.add(noArgConstructor(primary.simpleVisibility(), primary))
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

    override fun getOwnMethods(): List<KtLightMethod> = if (tooComplex) super.getOwnMethods() else _ownMethods.value

    private fun KtAnnotated.hasAnnotation(name: FqName) = support.findAnnotation(this, name) != null

    private fun KtCallableDeclaration.isConstOrJvmField() =
        hasModifier(CONST_KEYWORD) || isJvmField()

    private fun KtCallableDeclaration.isJvmField() = hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)

    override fun getInitializers(): Array<PsiClassInitializer> = emptyArray()

    override fun getContainingClass(): PsiClass? =
        if (tooComplex) super.getContainingClass()
        else ((classOrObject.parent as? KtClassBody)?.parent as? KtClassOrObject)?.let(KtLightClassForSourceDeclaration::create)

    override fun getParent(): PsiElement? = if (tooComplex) super.getParent() else containingClass ?: containingFile

    override fun getScope(): PsiElement? = if (tooComplex) super.getScope() else parent

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean =
        baseClass?.let { InheritanceImplUtil.isInheritorDeep(this, it, classToByPass) } ?: false

    override fun isDeprecated(): Boolean = _deprecated

    override fun copy(): KtLightClassImpl = KtUltraLightClass(classOrObject.copy() as KtClassOrObject, support)
}