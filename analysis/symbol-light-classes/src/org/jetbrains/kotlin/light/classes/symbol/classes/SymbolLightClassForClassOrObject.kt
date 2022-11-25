/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.getParentForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.basicIsEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class SymbolLightClassForClassOrObject(protected val classOrObject: KtClassOrObject, ktModule: KtModule) :
    SymbolLightClassBase(ktModule, classOrObject.manager),
    StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>> {
    private val classOrObjectSymbolPointer: KtSymbolPointer<KtClassOrObjectSymbol> = classOrObject.symbolPointer()

    protected fun <T> withClassOrObjectSymbol(action: KtAnalysisSession.(KtClassOrObjectSymbol) -> T): T {
        return classOrObjectSymbolPointer.withSymbol(ktModule, action)
    }

    protected fun <T> withNamedClassOrObjectSymbol(action: KtAnalysisSession.(KtNamedClassOrObjectSymbol) -> T): T =
        analyzeForLightClasses(ktModule) {
            action(requireNotNull(classOrObject.getNamedClassOrObjectSymbol()))
        }

    override val isTopLevel: Boolean get() = classOrObject.isTopLevel()
    internal val isCompanionObject: Boolean get() = classOrObject is KtObjectDeclaration && classOrObject.isCompanion()
    internal val isLocal: Boolean get() = classOrObject.isLocal
    internal val isNamedObject: Boolean get() = classOrObject is KtObjectDeclaration && !classOrObject.isCompanion()
    internal val isObject: Boolean get() = classOrObject is KtObjectDeclaration
    internal val isInterface: Boolean get() = classOrObject is KtClass && classOrObject.isInterface()
    internal val isAnnotation: Boolean get() = classOrObject is KtClass && classOrObject.isAnnotation()
    internal val isEnum: Boolean get() = classOrObject is KtClass && classOrObject.isEnum()

    private val _isDeprecated: Boolean by lazy {
        withClassOrObjectSymbol { it.hasDeprecatedAnnotation() }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    abstract override fun getModifierList(): PsiModifierList?
    abstract override fun getOwnFields(): List<KtLightField>
    abstract override fun getOwnMethods(): List<PsiMethod>

    private val _identifier: PsiIdentifier by lazy {
        KtLightIdentifier(this, classOrObject)
    }

    override fun getNameIdentifier(): PsiIdentifier? = _identifier

    abstract override fun getExtendsList(): PsiReferenceList?
    abstract override fun getImplementsList(): PsiReferenceList?

    private val _typeParameterList: PsiTypeParameterList? by lazy {
        hasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterPointer = classOrObjectSymbolPointer,
                ktModule = ktModule,
                ktDeclaration = classOrObject,
            )
        }
    }

    override fun hasTypeParameters(): Boolean = classOrObject.typeParameters.isNotEmpty()

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    private val _ownInnerClasses: List<SymbolLightClassBase> by lazyPub {
        withClassOrObjectSymbol {
            it.createInnerClasses(this@SymbolLightClassForClassOrObject, classOrObject)
        }
    }

    override fun getOwnInnerClasses(): List<PsiClass> = _ownInnerClasses

    override fun getTextOffset(): Int = classOrObject.textOffset
    override fun getStartOffsetInParent(): Int = classOrObject.startOffsetInParent
    override fun isWritable() = false
    override val kotlinOrigin: KtClassOrObject = classOrObject

    protected fun addMethodsFromCompanionIfNeeded(
        result: MutableList<KtLightMethod>,
    ): Unit = withNamedClassOrObjectSymbol { classOrObjectSymbol ->
        classOrObjectSymbol.companionObject?.run {
            val methods = getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtFunctionSymbol>()
                .filter { it.hasJvmStaticAnnotation() }

            createMethods(methods, result)

            val properties = getDeclaredMemberScope().getCallableSymbols().filterIsInstance<KtPropertySymbol>()
            properties.forEach { property ->
                createPropertyAccessors(
                    result,
                    property,
                    isTopLevel = false,
                    onlyJvmStatic = true
                )
            }
        }
    }

    protected fun addCompanionObjectFieldIfNeeded(result: MutableList<KtLightField>) {
        classOrObject.companionObjects.forEach {
            result.add(
                SymbolLightFieldForObject(
                    objectDeclaration = it,
                    containingClass = this@SymbolLightClassForClassOrObject,
                    name = it.name ?: return@forEach,
                    lightMemberOrigin = null,
                )
            )
        }
    }

    protected fun addFieldsFromCompanionIfNeeded(
        result: MutableList<KtLightField>,
    ): Unit = withNamedClassOrObjectSymbol { classOrObjectSymbol ->
        classOrObjectSymbol.companionObject
            ?.getDeclaredMemberScope()
            ?.getCallableSymbols()
            ?.filterIsInstance<KtPropertySymbol>()
            ?.applyIf(isInterface) {
                filter { it.isConstOrJvmField }
            }
            ?.mapTo(result) {
                SymbolLightFieldForProperty(
                    ktAnalysisSession = this,
                    propertySymbol = it,
                    fieldName = it.name.asString(),
                    containingClass = this@SymbolLightClassForClassOrObject,
                    lightMemberOrigin = null,
                    isTopLevel = false,
                    forceStatic = true,
                    takePropertyVisibility = it.isConstOrJvmField,
                )
            }

    }

    private val KtPropertySymbol.isConstOrJvmField: Boolean
        get() = isConst || hasJvmFieldAnnotation()

    private val KtPropertySymbol.isConst: Boolean
        get() = (this as? KtKotlinPropertySymbol)?.isConst == true

    override fun getNavigationElement(): PsiElement = classOrObject

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        basicIsEquivalentTo(this, another) ||
                another is PsiClass && qualifiedName != null && another.qualifiedName == qualifiedName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other?.safeAs<SymbolLightClassForClassOrObject>()?.classOrObject == classOrObject
    }

    override fun hashCode(): Int = classOrObject.hashCode()

    override fun getName(): String? = classOrObject.name

    override fun hasModifierProperty(@NonNls name: String): Boolean = modifierList?.hasModifierProperty(name) ?: false

    override fun isInterface(): Boolean = isInterface
    override fun isAnnotationType(): Boolean = isAnnotation
    override fun isEnum(): Boolean = isEnum

    override fun isValid(): Boolean = classOrObject.isValid

    override fun toString() = "${this::class.java.simpleName}:${classOrObject.getDebugText()}"

    override fun getUseScope(): SearchScope = classOrObject.useScope
    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = classOrObject.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = classOrObject.stub

    override val originKind: LightClassOriginKind get() = LightClassOriginKind.SOURCE

    override fun getQualifiedName() = classOrObject.fqName?.asString()

    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)
    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)

    override fun getContainingClass(): PsiClass? {
        val containingBody = classOrObject.parent as? KtClassBody
        val containingClass = containingBody?.parent as? KtClassOrObject
        containingClass?.let { return it.toLightClass() }
        return null
    }

    override fun getParent(): PsiElement? {
        if (isLocal) {
            return classOrObject.let(::getParentForLocalDeclaration)
        }

        return containingClass ?: containingFile
    }

    override fun getScope(): PsiElement? = parent

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean =
        baseClass?.let { InheritanceImplUtil.isInheritorDeep(this, it, classToByPass) } ?: false

    abstract override fun copy(): SymbolLightClassForClassOrObject
}
