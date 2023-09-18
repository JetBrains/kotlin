/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.ImplementationKindOwner
import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator

fun configureInterfacesAndAbstractClasses(elements: List<Element>) {
    object : InterfaceAndAbstractClassConfigurator() {
        override val elements: List<ImplementationKindOwner>
            get() = elements
    }.configureInterfacesAndAbstractClasses()
}
