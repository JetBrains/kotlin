/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.constructClassLikeTypeImport
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.FieldWithDefault
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.fir.tree.generator.standardClassIdsType
import org.jetbrains.kotlin.generators.tree.config.AbstractImplementationConfigurator

abstract class AbstractFirTreeImplementationConfigurator : AbstractImplementationConfigurator<Implementation, Element, FieldWithDefault>() {

    final override fun createImplementation(element: Element, name: String?) = Implementation(element, name)

    protected fun ImplementationContext.defaultNoReceivers() {
        defaultNull("explicitReceiver", "dispatchReceiver", "extensionReceiver")
    }

    protected fun ImplementationContext.defaultBuiltInType(type: String) {
        default("coneTypeOrNull") {
            value = "StandardClassIds.$type.constructClassLikeType()"
            isMutable = false
        }
        additionalImports(standardClassIdsType, constructClassLikeTypeImport)
    }

    protected fun ImplementationContext.noSource() {
        defaultNull("source", withGetter = true)
    }
}
