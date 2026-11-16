/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure

/**
 * Tells where the Kotlin declaration behind a light class comes from.
 *
 * A light class is a Java PSI view of a Kotlin declaration. Code that inspects a `JavaClass` or a
 * `KtLightClassMarker` reads this value to tell a source declaration from a compiled one.
 */
enum class LightClassOriginKind {
    /** The declaration comes from a Kotlin source file. */
    SOURCE,

    /** The declaration comes from a compiled library, such as a decompiled class file. */
    BINARY,
}
