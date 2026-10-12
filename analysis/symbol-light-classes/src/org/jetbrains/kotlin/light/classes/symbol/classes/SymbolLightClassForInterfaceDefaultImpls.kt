/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.utils.cachedValue
import org.jetbrains.kotlin.load.java.JvmAbi

/**
 * The `DefaultImpls` nested class of an interface, which holds the implementations of the interface members that are not compiled
 * to JVM `default` methods (`-jvm-default=disable`), or compatibility delegates to the JVM `default` methods (`-jvm-default=enable`),
 * as well as bridges for the inherited implementations. It has no symbol of its own and is backed by the interface symbol.
 */
internal class SymbolLightClassForInterfaceDefaultImpls(private val containingClass: SymbolLightClassForInterface) :
    SymbolLightClassForNamedClassLike(
        containingClass.classOrObjectDeclaration,
        containingClass.symbolPointer,
        containingClass.useSiteModule,
    ) {
    override fun getQualifiedName(): String? = containingClass.qualifiedName?.let { it + ".${JvmAbi.DEFAULT_IMPLS_CLASS_NAME}" }

    override fun getName() = JvmAbi.DEFAULT_IMPLS_CLASS_NAME
    override fun getParent() = containingClass

    override fun copy() = SymbolLightClassForInterfaceDefaultImpls(containingClass)

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return isEquivalentToByName(another)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightClassForInterfaceDefaultImpls && other.containingClass == containingClass

    override fun hashCode(): Int = containingClass.hashCode()

    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            modifiersBox = InitializedModifiersBox(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL),
        )
    }

    override fun classKind(): KaClassKind = KaClassKind.CLASS

    override fun hasTypeParameters(): Boolean = false
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean =
        baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT

    override fun getExtendsListTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY
    override fun getExtendsList(): PsiReferenceList? = null
    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY
    override fun getImplementsList(): PsiReferenceList? = null

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        throw IncorrectOperationException("Impossible to rename DefaultImpls")
    }

    override fun getContainingClass() = containingClass

    override val ownConstructors: Array<PsiMethod> get() = PsiMethod.EMPTY_ARRAY

    override fun getOwnInnerClasses() = emptyList<PsiClass>()

    /**
     * Holds static methods for the non-abstract instance members declared in the interface and bridges for the implementations
     * inherited from super-interfaces, see [inheritedDefaultImplsCallables].
     *
     * The methods for the declared members depend on the `-jvm-default` mode:
     * - With `-jvm-default=disable`, these are the implementations of all such members, including the private ones.
     * - With `-jvm-default=enable`, these are compatibility delegates to the JVM `default` methods of the non-private members, while
     *   the private members stay private JVM `default` methods of the interface class.
     * - With `-jvm-default=no-compatibility`, all implementations stay JVM `default` methods of the interface class, and `DefaultImpls`
     *   holds only the inherited bridges.
     *
     * The members of the companion block and the `@JvmStatic` members of the companion object are static methods of the interface
     * class itself, which [SymbolLightClassForInterface.getOwnMethods] provides.
     */
    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassSymbol { classSymbol ->
            val jvmDefaultMode = jvmDefaultMode
            val declaredImplementations = when (jvmDefaultMode) {
                JvmDefaultMode.NO_COMPATIBILITY -> emptySequence()
                else -> {
                    val includePrivateMembers = !jvmDefaultMode.isEnabled
                    classSymbol.combinedDeclaredMemberScope.callables.filter {
                        !it.isCompanion &&
                                it.modality != KaSymbolModality.ABSTRACT &&
                                (includePrivateMembers || it.visibility != KaSymbolVisibility.PRIVATE)
                    }
                }
            }

            val inheritedImplementations = inheritedDefaultImplsCallables(classSymbol, jvmDefaultMode)

            val result = mutableListOf<PsiMethod>()
            createMethods(this@SymbolLightClassForInterfaceDefaultImpls, declaredImplementations + inheritedImplementations, result)
            result
        }
    }

    override fun getOwnFields(): List<PsiField> = emptyList()
}
