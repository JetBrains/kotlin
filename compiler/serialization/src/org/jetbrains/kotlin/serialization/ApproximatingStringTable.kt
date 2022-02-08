/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils

class ApproximatingStringTable : StringTableImpl() {
    override fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId? {
        return if (DescriptorUtils.isLocal(descriptor)) {
            ClassId.topLevel(StandardNames.FqNames.any.toSafe())
        } else {
            super.getLocalClassIdReplacement(descriptor)
        }
    }

    override val isLocalClassIdReplacementKeptGeneric: Boolean
        get() = false
}
