/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

fun CompanionObjectMapping.isMappedIntrinsicCompanionObject(classDescriptor: ClassDescriptor): Boolean =
    DescriptorUtils.isCompanionObject(classDescriptor) && classDescriptor.classId?.outerClassId in classIds

fun CompanionObjectMapping.isMappedIntrinsicCompanionObjectClassId(classId: ClassId): Boolean =
    classId.outerClassId in classIds