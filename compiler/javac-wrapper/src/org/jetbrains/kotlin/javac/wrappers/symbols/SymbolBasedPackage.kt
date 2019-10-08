/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.javac.wrappers.symbols

import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.MapBasedJavaAnnotationOwner
import javax.lang.model.element.PackageElement

interface SymbolBasedPackage : JavaPackage, MapBasedJavaAnnotationOwner {
    val element: PackageElement
}