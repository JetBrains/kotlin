/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

/**
 * Describes the location where a [KaSymbol] is declared in code.
 *
 * #### Example
 *
 * ```kotlin
 * package my.pack // TOP_LEVEL
 *
 * class TopLevelClass { // TOP_LEVEL
 *   class NestedClass { // CLASS
 *     fun memberFunction() { // CLASS
 *       val foo = 4 // LOCAL
 *     }
 *   }
 * }
 *
 * val topLevelProperty: Int // TOP_LEVEL
 *   get() = 0 // PROPERTY
 *
 * fun topLevelFunction() { // TOP_LEVEL
 *   class LocalClass { // LOCAL
 *     val memberProperty = 0 // CLASS
 *
 *     fun memberFunction() { // CLASS
 *       fun localFunction() {} // LOCAL
 *     }
 *   }
 * }
 * ```
 */
public enum class KaSymbolLocation {
    /**
     * Symbols which are not a part of other symbols.
     *
     * Examples: [KaFileSymbol], [KaScriptSymbol], [KaPackageSymbol], and top-level declarations.
     */
    TOP_LEVEL,

    /**
     * Symbols which are a part of a [KaClassSymbol].
     */
    CLASS,

    /**
     * Symbols which are a part of a [KaPropertySymbol], such as [KaPropertyAccessorSymbol] and [KaBackingFieldSymbol].
     */
    PROPERTY,

    /**
     * Symbols which are defined directly inside a body.
     *
     * [LOCAL] is not propagated to the members of local classes. So the member declaration of a local class has [CLASS] as its location and
     * not [LOCAL].
     */
    LOCAL,
}

/**
 * Indicates whether a [KaSymbol] is at the top level.
 *
 * A symbol is considered top-level if it is not a part of other symbols.
 *
 * @see KaSymbolLocation.TOP_LEVEL
 */
public val KaSymbol.isTopLevel: Boolean get() = location == KaSymbolLocation.TOP_LEVEL

/**
 * Indicates whether a symbol is defined locally within a body.
 *
 * @see KaSymbolLocation.LOCAL
 */
public val KaSymbol.isLocal: Boolean get() = location == KaSymbolLocation.LOCAL
