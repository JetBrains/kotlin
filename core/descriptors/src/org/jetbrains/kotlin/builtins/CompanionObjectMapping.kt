/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import java.util.*

object CompanionObjectMapping {
    private val classIds =
        (PrimitiveType.NUMBER_TYPES.map(KotlinBuiltIns::getPrimitiveFqName) +
                KotlinBuiltIns.FQ_NAMES.string.toSafe() +
                KotlinBuiltIns.FQ_NAMES._boolean.toSafe() +
                KotlinBuiltIns.FQ_NAMES._enum.toSafe()).mapTo(linkedSetOf<ClassId>(), ClassId::topLevel)

    fun allClassesWithIntrinsicCompanions(): Set<ClassId> =
        Collections.unmodifiableSet(classIds)

    fun isMappedIntrinsicCompanionObject(classDescriptor: ClassDescriptor): Boolean =
        DescriptorUtils.isCompanionObject(classDescriptor) && classDescriptor.classId?.outerClassId in classIds
}
