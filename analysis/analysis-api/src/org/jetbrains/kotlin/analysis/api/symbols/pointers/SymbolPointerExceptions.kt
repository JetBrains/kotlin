/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

public class CanNotCreateSymbolPointerForLocalLibraryDeclarationException(description: String) :
    IllegalStateException("Could not create a symbol pointer for local symbol $description")

public class WrongSymbolForSamConstructor(symbolKind: String) :
    IllegalStateException(
        "For symbol with kind = KtSymbolKind.SAM_CONSTRUCTOR, KtSamConstructorSymbol should be created, but was $symbolKind"
    )
