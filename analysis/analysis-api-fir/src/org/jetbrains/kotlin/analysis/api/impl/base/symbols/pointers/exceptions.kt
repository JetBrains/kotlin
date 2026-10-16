/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import kotlin.reflect.KClass

@KaImplementationDetail
class KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(identifier: String) :
    IllegalStateException("Could not create a symbol pointer for local symbol $identifier") {
    constructor(klass: KClass<*>) : this(klass.java.simpleName)
}

@KaImplementationDetail
class KaUnsupportedSymbolLocation(identifier: String, location: KaSymbolLocation) : IllegalStateException(
    "For symbol with kind = KaSymbolLocation.${location.name} was $identifier"
) {
    constructor(clazz: KClass<*>, location: KaSymbolLocation) : this(clazz.java.simpleName, location)
}
