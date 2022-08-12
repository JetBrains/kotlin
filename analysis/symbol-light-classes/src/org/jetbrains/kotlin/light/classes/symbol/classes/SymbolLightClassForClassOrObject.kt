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
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.asJava.classes.getParentForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.light.classes.symbol.SymbolLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.allowLightClassesOnEdt
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.basicIsEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

context(KtAnalysisSession)
abstract class SymbolLightClassForClassOrObject(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : SymbolLightClassBase(manager),
    StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>> {

    override val isTopLevel: Boolean = classOrObjectSymbol.symbolKind == KtSymbolKind.TOP_LEVEL

    internal val isCompanionObject: Boolean
        get() = classOrObjectSymbol.classKind == KtClassKind.COMPANION_OBJECT

    private val _isDeprecated: Boolean by lazyPub {
        classOrObjectSymbol.hasDeprecatedAnnotation()
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    abstract override fun getModifierList(): PsiModifierList?
    abstract override fun getOwnFields(): List<KtLightField>
    abstract override fun getOwnMethods(): List<PsiMethod>

    private val _identifier: PsiIdentifier by lazyPub {
        SymbolLightIdentifier(this, classOrObjectSymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier? = _identifier

    abstract override fun getExtendsList(): PsiReferenceList?
    abstract override fun getImplementsList(): PsiReferenceList?

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        hasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterList = classOrObjectSymbol,
            )
        }
    }

    override fun hasTypeParameters(): Boolean =
        classOrObjectSymbol.typeParameters.isNotEmpty()

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> =
        _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    private val _ownInnerClasses: List<SymbolLightClassBase> by lazyPub {
        classOrObjectSymbol.createInnerClasses(manager, this, kotlinOrigin)
    }

    override fun getOwnInnerClasses(): List<PsiClass> = _ownInnerClasses

    override fun getTextOffset(): Int = kotlinOrigin?.textOffset ?: 0
    override fun getStartOffsetInParent(): Int = kotlinOrigin?.startOffsetInParent ?: 0
    override fun isWritable() = false
    override val kotlinOrigin: KtClassOrObject? = classOrObjectSymbol.psi as? KtClassOrObject

    protected fun addMethodsFromCompanionIfNeeded(result: MutableList<KtLightMethod>) {
        classOrObjectSymbol.companionObject?.run {
            val methods = getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtFunctionSymbol>()
                .filter { it.hasJvmStaticAnnotation() }
            createMethods(methods, result)

            val properties = getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtPropertySymbol>()
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
        classOrObjectSymbol.companionObject?.run {
            result.add(
                SymbolLightFieldForObject(
                    objectSymbol = this,
                    containingClass = this@SymbolLightClassForClassOrObject,
                    name = name.asString(),
                    lightMemberOrigin = null
                )
            )
        }
    }

    protected fun addFieldsFromCompanionIfNeeded(result: MutableList<KtLightField>) {
        classOrObjectSymbol.companionObject?.run {
            getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtPropertySymbol>()
                .applyIf(isInterface) {
                    filter { it.isConstOrJvmField }
                }
                .mapTo(result) {
                    SymbolLightFieldForProperty(
                        propertySymbol = it,
                        fieldName = it.name.asString(),
                        containingClass = this@SymbolLightClassForClassOrObject,
                        lightMemberOrigin = null,
                        isTopLevel = false,
                        forceStatic = true,
                        takePropertyVisibility = it.isConstOrJvmField
                    )
                }
        }
    }

    private val KtPropertySymbol.isConstOrJvmField: Boolean
        get() = isConst || hasJvmFieldAnnotation()

    private val KtPropertySymbol.isConst: Boolean
        get() = (this as? KtKotlinPropertySymbol)?.isConst == true

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: this

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        basicIsEquivalentTo(this, another) ||
                another is PsiClass && qualifiedName != null && another.qualifiedName == qualifiedName

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun getName(): String? = allowLightClassesOnEdt { classOrObjectSymbol.name.asString() }

    override fun hasModifierProperty(@NonNls name: String): Boolean = modifierList?.hasModifierProperty(name) ?: false

    abstract override fun isInterface(): Boolean

    abstract override fun isAnnotationType(): Boolean

    abstract override fun isEnum(): Boolean

    override fun isValid(): Boolean = kotlinOrigin?.isValid ?: true

    override fun toString() =
        "${this::class.java.simpleName}:${kotlinOrigin?.getDebugText()}"

    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: TODO()
    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = kotlinOrigin?.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = kotlinOrigin?.stub

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override fun getQualifiedName() = kotlinOrigin?.fqName?.asString()

    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)
    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)

    override fun getContainingClass(): PsiClass? {
        val containingBody = kotlinOrigin?.parent as? KtClassBody
        val containingClass = containingBody?.parent as? KtClassOrObject
        containingClass?.let { return it.toLightClass() }
        return null
    }

    override fun getParent(): PsiElement? {
        if (classOrObjectSymbol.safeAs<KtSymbolWithKind>()?.symbolKind == KtSymbolKind.LOCAL) {
            return kotlinOrigin?.let(::getParentForLocalDeclaration)
        }
        return containingClass ?: containingFile
    }

    override fun getScope(): PsiElement? = parent

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean =
        baseClass?.let { InheritanceImplUtil.isInheritorDeep(this, it, classToByPass) } ?: false

    abstract override fun copy(): SymbolLightClassForClassOrObject
}
