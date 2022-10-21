/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class PartialLinkageSupportImpl(
    builtIns: IrBuiltIns,
    messageLogger: IrMessageLogger
) : PartialLinkageSupport {
    private val usedClassifierSymbols = UsedClassifierSymbols()
    private val classifierUsageMarker = ClassifierUsageMarker(usedClassifierSymbols)

    // Keep this handler here for the whole duration of IR linker life cycle. This is necessary to have
    // stable reference equality (===) for the marker IR type.
    private val unlinkedMarkerTypeHandler = UnlinkedMarkerTypeHandlerImpl(builtIns)
    private val unlinkedDeclarationsProcessor =
        UnlinkedDeclarationsProcessor(builtIns, usedClassifierSymbols, unlinkedMarkerTypeHandler, messageLogger)

    override val partialLinkageEnabled get() = true

    override fun markUsedClassifiersExcludingUnlinkedFromFakeOverrideBuilding(fakeOverrideBuilder: FakeOverrideBuilder): Unit =
        with(classifierUsageMarker) {
            val entries = fakeOverrideBuilder.fakeOverrideCandidates
            if (entries.isEmpty()) return

            val toRemove = buildSet {
                for (clazz in entries.keys) {
                    if (clazz.symbol.isUnlinkedClassifier(visited = hashSetOf())) {
                        this += clazz
                    }
                }
            }

            entries -= toRemove
        }

    override fun markUsedClassifiersInInlineLazyIrFunction(function: IrFunction) {
        function.acceptChildrenVoid(classifierUsageMarker)
    }

    override fun processUnlinkedDeclarations(lazyRoots: () -> List<IrElement>) {
        unlinkedDeclarationsProcessor.addLinkageErrorIntoUnlinkedClasses()

        val roots = lazyRoots()

        val signatureTransformer = unlinkedDeclarationsProcessor.signatureTransformer()
        roots.forEach { it.transformChildrenVoid(signatureTransformer) }

        val usageTransformer = unlinkedDeclarationsProcessor.usageTransformer()
        roots.forEach { it.transformChildrenVoid(usageTransformer) }
    }
}
