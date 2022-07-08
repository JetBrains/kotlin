/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

open class ConeIdShortRenderer : ConeIdRenderer() {
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.relativeClassName.asString())
    }

    override fun renderCallableId(callableId: CallableId) {
        builder.append(callableId.callableName)
    }
}