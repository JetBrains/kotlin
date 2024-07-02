/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

abstract class SymbolLightClassForClassLike<SType : KaClassSymbol> protected constructor(
    internal val classOrObjectDeclaration: KtClassOrObject?,
    internal val classSymbolPointer: KaSymbolPointer<SType>,
    ktModule: KaModule,
    manager: PsiManager,
) : SymbolLightClassBase(ktModule, manager),
    StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>> {
    constructor(
        ktAnalysisSession: KaSession,
        ktModule: KaModule,
        classSymbol: SType,
        manager: PsiManager,
    ) : this(
        classOrObjectDeclaration = classSymbol.sourcePsiSafe(),
        classSymbolPointer = with(ktAnalysisSession) {
            @Suppress("UNCHECKED_CAST")
            classSymbol.createPointer() as KaSymbolPointer<SType>
        },
        ktModule = ktModule,
        manager = manager,
    )

    override fun modificationTrackerForClassInnerStuff(): List<ModificationTracker> {
        return classOrObjectDeclaration?.modificationTrackerForClassInnerStuff() ?: super.modificationTrackerForClassInnerStuff()
    }

    override val kotlinOrigin: KtClassOrObject? get() = classOrObjectDeclaration

    internal inline fun <T> withClassSymbol(crossinline action: KaSession.(SType) -> T): T =
        classSymbolPointer.withSymbol(ktModule, action)

    override val isTopLevel: Boolean by lazyPub {
        classOrObjectDeclaration?.isTopLevel() ?: withClassSymbol { it.isTopLevel }
    }

    private val _isDeprecated: Boolean by lazyPub {
        withClassSymbol { it.hasDeprecatedAnnotation() }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    abstract override fun getModifierList(): PsiModifierList?

    abstract override fun getOwnFields(): List<KtLightField>
    abstract override fun getOwnMethods(): List<PsiMethod>

    override fun getNameIdentifier(): PsiIdentifier? = KtLightIdentifier(this, classOrObjectDeclaration)

    abstract override fun getExtendsList(): PsiReferenceList?

    abstract override fun getImplementsList(): PsiReferenceList?

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        hasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterPointer = classSymbolPointer,
                ktModule = ktModule,
                ktDeclaration = classOrObjectDeclaration,
            )
        }
    }

    override fun hasTypeParameters(): Boolean =
        hasTypeParameters(ktModule, classOrObjectDeclaration, classSymbolPointer)

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> = cachedValue {
        withClassSymbol {
            it.createInnerClasses(manager, this@SymbolLightClassForClassLike, classOrObjectDeclaration)
        }
    }

    override fun getTextOffset(): Int = classOrObjectDeclaration?.textOffset ?: -1

    override fun getStartOffsetInParent(): Int = classOrObjectDeclaration?.startOffsetInParent ?: -1
    override fun isWritable() = false
    override fun getNavigationElement(): PsiElement = classOrObjectDeclaration ?: this

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        isEquivalentToByName(another) ||
                isOriginEquivalentTo(another)

    protected fun isEquivalentToByName(another: PsiElement?): Boolean = basicIsEquivalentTo(this, another) ||
            another is PsiClass && qualifiedName != null && another.qualifiedName == qualifiedName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightClassForClassLike<*> || other.ktModule != ktModule || other.manager != manager) return false
        if (classOrObjectDeclaration != null || other.classOrObjectDeclaration != null) {
            return other.classOrObjectDeclaration == classOrObjectDeclaration
        }

        return compareSymbolPointers(classSymbolPointer, other.classSymbolPointer)
    }

    override fun hashCode(): Int = classOrObjectDeclaration.hashCode()

    override fun getName(): String? = classOrObjectDeclaration?.name ?: withClassSymbol {
        it.name?.asString()
    }

    override fun hasModifierProperty(@NonNls name: String): Boolean = modifierList?.hasModifierProperty(name) ?: false

    abstract fun classKind(): KaClassKind
    override fun isInterface(): Boolean = classKind().let { it == KaClassKind.INTERFACE || it == KaClassKind.ANNOTATION_CLASS }
    override fun isAnnotationType(): Boolean = classKind() == KaClassKind.ANNOTATION_CLASS
    override fun isEnum(): Boolean = classKind() == KaClassKind.ENUM_CLASS

    override fun isValid(): Boolean = classOrObjectDeclaration?.isValid ?: classSymbolPointer.isValid(ktModule)

    override fun toString() = "${this::class.java.simpleName}:${classOrObjectDeclaration?.getDebugText()}"

    override fun getUseScope(): SearchScope = classOrObjectDeclaration?.useScope ?: GlobalSearchScope.projectScope(project)
    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = classOrObjectDeclaration?.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = classOrObjectDeclaration?.stub

    override val originKind: LightClassOriginKind get() = LightClassOriginKind.SOURCE

    override fun getQualifiedName(): String? {
        val classOrObjectFqName = classOrObjectDeclaration?.fqName
            ?: withClassSymbol { s -> s.classId?.asSingleFqName() }

        return classOrObjectFqName?.toString()
    }

    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)
    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)

    private val _containingClass: PsiClass? by lazyPub {
        val containingBody = classOrObjectDeclaration?.parent
        when (val parent = containingBody?.parent) {
            is KtClassOrObject -> parent.toLightClass()
            is KtScript -> parent.toLightClass()
            null -> withClassSymbol { s ->
                (s.containingDeclaration as? KaNamedClassSymbol)?.let { createLightClassNoCache(it, ktModule, manager) }
            }
            else -> null
        }
    }

    override fun getContainingClass(): PsiClass? = _containingClass

    private val _containingFile: PsiFile? by lazyPub {
        super.getContainingFile() ?: containingClass?.containingFile
    }

    override fun getContainingFile(): PsiFile? = _containingFile

    abstract override fun getParent(): PsiElement?
    override fun getScope(): PsiElement? = parent

    override fun isInheritorDeep(baseClass: PsiClass, classToByPass: PsiClass?): Boolean =
        InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass)

    abstract override fun copy(): SymbolLightClassForClassLike<*>
}
