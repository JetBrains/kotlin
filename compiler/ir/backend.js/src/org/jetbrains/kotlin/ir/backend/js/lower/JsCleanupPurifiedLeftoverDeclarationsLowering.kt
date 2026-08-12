/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.isLeftoverAfterObjectPurification
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

/**
 * Removes declarations marked for removal in [PurifyObjectInstanceGettersLowering] and [InlineObjectsWithPureInitializationLowering] lowerings.
 *
 * Due to the fact that in JS DCE runs before optimization lowerings mentioned above, we need to clean some declarations after removing usages of them.
 * These declarations currently include:
 *   - `_getInstance` declarations, already replaced with `_instance` field related to purified objects.
 *   - `static_init` declarations related to the purified objects.
 *
 * The lowering can be removed once more general purpose DCE becomes available after optimization lowering execution.
 */
@PhasePrerequisites(
    PurifyObjectInstanceGettersLowering::class,
    InlineObjectsWithPureInitializationLowering::class
)
class JsCleanupPurifiedLeftoverDeclarationsLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.isLeftoverAfterObjectPurification)
            return emptyList()

        return null
    }
}
