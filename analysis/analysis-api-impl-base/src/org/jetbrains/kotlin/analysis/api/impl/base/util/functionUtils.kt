/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.util.OperatorNameConventions

@KaImplementationDetail
val kotlinFunctionInvokeCallableIds = (0..23).flatMapTo(hashSetOf()) { arity ->
    listOf(
        CallableId(StandardNames.getFunctionClassId(arity), OperatorNameConventions.INVOKE),
        CallableId(StandardNames.getSuspendFunctionClassId(arity), OperatorNameConventions.INVOKE)
    )
}
