/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.render

class JsUniqIdClashTracker: UniqIdClashTracker {
    private val commitedUniqIds = mutableMapOf<UniqId, IrDeclaration>()

    override fun commit(declaration: IrDeclaration, uniqId: UniqId) {
        if (uniqId.isLocal) return // don't track local ids

        if (uniqId in commitedUniqIds) {
            val clashedDeclaration = commitedUniqIds[uniqId]!!
            // TODO: handle clashes properly
            error("UniqId clash: $uniqId; Existed declaration ${clashedDeclaration.render()} clashed with new ${declaration.render()}")
        }

        commitedUniqIds[uniqId] = declaration
    }
}

class JsGlobalDeclarationTable(builtIns: IrBuiltIns) : GlobalDeclarationTable(JsMangler, JsUniqIdClashTracker()) {
    init {
        loadKnownBuiltins(builtIns, PUBLIC_LOCAL_UNIQ_ID_EDGE)
    }
}