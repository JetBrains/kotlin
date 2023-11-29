/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.model

import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.ImplementationKindOwner
import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.util.Node
import org.jetbrains.kotlin.generators.util.solveGraphForClassVsInterface

fun configureInterfacesAndAbstractClasses(elements: List<Element>) {
    object : InterfaceAndAbstractClassConfigurator(elements) {
    }.configureInterfacesAndAbstractClasses()
}
