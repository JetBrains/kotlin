/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.constructClassLikeTypeImport
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.printer.call
import org.jetbrains.kotlin.fir.tree.generator.standardClassIdsType
import org.jetbrains.kotlin.generators.tree.AbstractImplementationConfigurator

abstract class AbstractFirTreeImplementationConfigurator : AbstractImplementationConfigurator<Implementation, Element, FieldWithDefault>() {

    override fun createImplementation(element: Element, name: String?) = Implementation(element, name)

    protected fun ImplementationContext.defaultNoReceivers() {
        defaultNull("explicitReceiver", "dispatchReceiver", "extensionReceiver")
    }

    protected fun ImplementationContext.defaultBuiltInType(type: String) {
        default("coneTypeOrNull") {
            value = "StandardClassIds.$type.constructClassLikeType()"
            isMutable = false
        }
        useTypes(standardClassIdsType, constructClassLikeTypeImport)
    }

    protected fun ImplementationContext.noSource() {
        defaultNull("source", withGetter = true)
    }
}
