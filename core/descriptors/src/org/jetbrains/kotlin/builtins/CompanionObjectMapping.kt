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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.Collections

public object CompanionObjectMapping {
    private val classes = linkedSetOf<ClassDescriptor>()

    init {
        val builtIns = KotlinBuiltIns.getInstance()
        for (type in PrimitiveType.NUMBER_TYPES) {
            classes.add(builtIns.getPrimitiveClassDescriptor(type))
        }
        classes.add(builtIns.getString())
        classes.add(builtIns.getEnum())
    }

    @JvmStatic
    public fun allClassesWithIntrinsicCompanions(): Set<ClassDescriptor> =
            Collections.unmodifiableSet(classes)

    @JvmStatic
    public fun hasMappingToObject(classDescriptor: ClassDescriptor): Boolean {
        return DescriptorUtils.isCompanionObject(classDescriptor) &&
               classDescriptor.getContainingDeclaration() in classes
    }
}
