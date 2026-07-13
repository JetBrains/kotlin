/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.checker

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.diagnostics.warning2
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.name.FqName
import kotlin.getValue

object StaticInitializationDiagnostics : KtDiagnosticsContainer() {
    val POSSIBLE_INITIALIZATION_DEADLOCK by warning1<PsiElement, List<FqName>>()
    val POSSIBLY_UNINITIALIZED_PROPERTY by warning2<PsiElement, FqName, List<FqName>>()
    val POSSIBLY_UNINITIALIZED_ENUM_ENTRY by warning2<PsiElement, IrEnumEntrySymbol, List<FqName>>()
    val ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY by warning1<PsiElement, FqName>()
    val ACCESSING_POSSIBLY_UNINITIALIZED_ENUM_ENTRY by warning1<PsiElement, IrEnumEntrySymbol>()
    val POSSIBLE_CYCLIC_ACCESS by warning1<PsiElement, IrBindableSymbol<*, out IrDeclaration>>()
    val ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE by warning1<PsiElement, FqName>()
    val ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS by warning2<PsiElement, FqName, IrBindableSymbol<*, out IrDeclarationWithName>>()
    val CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS by warning1<PsiElement, FqName>()


    override fun getRendererFactory(): BaseDiagnosticRendererFactory = object : BaseDiagnosticRendererFactory() {
        override val MAP by KtDiagnosticFactoryToRendererMap("Static Initialization") {
            it.put(
                POSSIBLE_INITIALIZATION_DEADLOCK,
                "Possible initialization deadlock with ''{0}''.",
                Renderer { classes -> classes.joinToString(transform = FqName::asString) },
            )
            it.put(
                POSSIBLY_UNINITIALIZED_PROPERTY,
                "Possibly uninitialized property ''{0}'' due to mutually dependent (direct or indirect) accesses ''{1}''.",
                Renderer(FqName::asString),
                Renderer { classes ->
                    if (classes.isEmpty()) "between the declarations of its containing class"
                    else classes.joinToString(prefix = "in ", transform = FqName::asString)
                },
            )
            it.put(
                POSSIBLY_UNINITIALIZED_ENUM_ENTRY,
                "Possibly uninitialized enum entry ''{0}'' due to mutually dependent (direct or indirect) accesses ''{1}''.",
                Renderer { enumEntrySymbol -> enumEntrySymbol.owner.name.asString() },
                Renderer { classes ->
                    if (classes.isEmpty()) "between the declarations of its containing class"
                    else classes.joinToString(prefix = "in ", transform = FqName::asString)
                },
            )
            it.put(
                ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY,
                "The expression accesses (either directly or indirectly) the property ''{0}'' when it is possibly uninitialized.",
                Renderer(FqName::asString),
            )
            it.put(
                ACCESSING_POSSIBLY_UNINITIALIZED_ENUM_ENTRY,
                "The expression accesses (either directly or indirectly) the enum entry ''{0}'' when it is possibly uninitialized.",
                Renderer { enumEntrySymbol -> enumEntrySymbol.owner.name.asString() },
            )
            it.put(
                POSSIBLE_CYCLIC_ACCESS,
                "The expression accesses (either directly or indirectly) the declaration ''{0}'' that is possibly uninitialized due to cyclic access in its own initializer.",
                Renderer { declSymbol -> (declSymbol.owner as? IrDeclarationWithName)?.name?.asString() ?: "???" },
            )
            it.put(
                ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE,
                "The expression accesses (either directly or indirectly) the object ''{0}'' when it is not fully (statically) initialized (due to mutual static dependencies), any static access to its declarations may cause an NPE.",
                Renderer(FqName::asString),
            )
            it.put(
                ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS,
                "The expression accesses (either directly or indirectly) the declaration ''{1}'' of a class ''{0}'' when it is not fully statically initialized (due to mutual static dependencies), any static access to its declarations may cause an NPE.",
                Renderer(FqName::asString),
                Renderer { declSymbol -> declSymbol.owner.name.asString() },
            )
            it.put(
                CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS,
                "The constructor call creates possible static initialization deadlock due to mutual static dependencies of its constructing class ''{0}''.",
                Renderer(FqName::asString),
            )
        }
    }
}
