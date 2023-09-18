/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.ImplementationKindOwner
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator

private class FirInterfaceAndAbstractClassConfigurator(builder: AbstractFirTreeBuilder) : InterfaceAndAbstractClassConfigurator() {

    override val elements = (builder.elements + builder.elements.flatMap { it.allImplementations }).map { it.origin }

    override val ImplementationKindOwner.origin: ImplementationKindOwner
        get() = if (this is ImplementationWithArg) implementation else this

    override fun shouldBeFinalClass(element: ImplementationKindOwner, allParents: Set<ImplementationKindOwner>): Boolean =
        element is Implementation && element !in allParents
}

fun configureInterfacesAndAbstractClasses(builder: AbstractFirTreeBuilder) {
    FirInterfaceAndAbstractClassConfigurator(builder).configureInterfacesAndAbstractClasses()
}
