/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.name.Name

internal val renameFieldsPhase = makeIrFilePhase<CommonBackendContext>(
    { RenameFieldsLowering() },
    name = "RenameFields",
    description = "Rename private fields (including fields copied from companion object) to avoid JVM declaration clash"
)

private class RenameFieldsLowering : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val fields = irClass.fields.toMutableList()
        fields.sortBy {
            when {
                // We never rename public ABI fields (public and protected visibility) since they are accessible from Java
                // even in cases when Kotlin code would prefer an accessor. (And in some cases, such as enum entries and const
                // fields, Kotlin references the fields directly too.) Therefore we consider such fields first, in order to make
                // sure it'll claim its original name. There can be multiple such fields, in which case they will cause a platform
                // declaration clash if they map to the same JVM type - nothing we can do about that.
                it.visibility.isPublicAPI -> 0
                // If there are non-public non-static and static (moved from companion) fields with the same name, we try to make
                // static properties retain their original names first, since this is what the old JVM backend did. However this
                // can easily be changed without any major binary compatibility consequences (ignoring Java reflection).
                it.isStatic -> 1
                else -> 2
            }
        }

        val count = hashMapOf<Name, Int>()
        for (field in fields) {
            val oldName = field.name
            val index = count[oldName] ?: 0
            if (index != 0 && !field.visibility.isPublicAPI) {
                field.name = Name.identifier("$oldName$$index")
            }
            count[oldName] = index + 1
        }
    }
}
