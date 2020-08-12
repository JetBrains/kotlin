/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.ClassId

val KtType.isUnit: Boolean get() = isClassTypeWithClassId(DefaultTypeClassIds.UNIT)

fun KtType.isClassTypeWithClassId(classId: ClassId): Boolean {
    if (this !is KtClassType) return false
    return this.classId == classId
}

private object DefaultTypeClassIds {
    val UNIT = ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.unit.toSafe())
}