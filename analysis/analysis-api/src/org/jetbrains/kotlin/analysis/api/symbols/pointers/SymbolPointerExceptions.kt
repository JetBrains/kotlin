/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import kotlin.reflect.KClass

public class CanNotCreateSymbolPointerForLocalLibraryDeclarationException(identifier: String) :
    IllegalStateException("Could not create a symbol pointer for local symbol $identifier") {
    public constructor(klass: KClass<*>) : this(klass.java.simpleName)
}

public class UnsupportedSymbolKind(identifier: String, kind: KtSymbolKind) : IllegalStateException(
    "For symbol with kind = KtSymbolKind.${kind.name} was $identifier"
) {
    public constructor(clazz: KClass<*>, kind: KtSymbolKind) : this(clazz.java.simpleName, kind)
}
