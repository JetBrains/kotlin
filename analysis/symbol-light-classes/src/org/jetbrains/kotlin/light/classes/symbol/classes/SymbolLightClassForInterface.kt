/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.utils.cachedValue
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class SymbolLightClassForInterface : SymbolLightClassForInterfaceOrAnnotationClass {
    constructor(
        useSiteModule: KaModule,
        classSymbol: KaNamedClassSymbol,
    ) : super(
        useSiteModule = useSiteModule,
        classSymbol = classSymbol,
    ) {
        require(classSymbol.classKind == KaClassKind.INTERFACE)
    }

    private constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        useSiteModule: KaModule,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        useSiteModule = useSiteModule,
    )

    /**
     * Includes the members declared in the interface and in its companion block, and the `@JvmStatic` members of its companion
     * object, which are static methods of the interface class. The JVM shape of a member depends on the `-jvm-default` mode,
     * mirroring `org.jetbrains.kotlin.backend.jvm.lower.InterfaceLowering`:
     * - A member without a body is an abstract method in any mode.
     * - With JVM default methods enabled, a member with a body is a JVM `default` method, even if it is private.
     * - With `-jvm-default=disable`, the implementation of a member with a body is moved to `DefaultImpls`. A non-private member
     *   leaves an abstract stub in the interface class, and a private member leaves nothing, so private members are excluded.
     * - Members of the companion block are static methods of the interface class in any mode, so private ones are kept.
     */
    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassSymbol { classSymbol ->
            val result = mutableListOf<PsiMethod>()

            val includePrivateMembers = jvmDefaultMode.isEnabled
            val visibleDeclarations = classSymbol.combinedDeclaredMemberScope.callables.filter {
                includePrivateMembers || it.isCompanion || it.visibility != KaSymbolVisibility.PRIVATE
            }

            createMethods(this@SymbolLightClassForInterface, visibleDeclarations, result)
            addMethodsFromCompanionIfNeeded(result, classSymbol)

            result
        }
    }

    override fun copy(): SymbolLightClassForInterface =
        SymbolLightClassForInterface(classOrObjectDeclaration, symbolPointer, useSiteModule)

    private val _extendsList: PsiReferenceList by lazyPub {
        withClassSymbol { classSymbol ->
            createInheritanceList(this@SymbolLightClassForInterface, forExtendsList = true, classSymbol.superTypes)
        }
    }

    override fun getExtendsList(): PsiReferenceList = _extendsList
    override fun classKind(): KaClassKind = KaClassKind.INTERFACE
}
