/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils



val CONTINUATION_INTERFACE_CLASS_ID = ClassId.topLevel(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE)