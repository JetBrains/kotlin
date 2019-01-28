/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.model

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.contracts.model.structure.ESConstants

class ESComponents(
    val builtIns: KotlinBuiltIns
) {
    val constants: ESConstants = ESConstants(builtIns)
}
