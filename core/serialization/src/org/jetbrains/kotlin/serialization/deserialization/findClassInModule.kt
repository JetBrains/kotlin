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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.ClassId

public fun ModuleDescriptor.findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? {
    val packageViewDescriptor = getPackage(classId.getPackageFqName()) ?: return null
    val segments = classId.getRelativeClassName().pathSegments()
    val topLevelClass = packageViewDescriptor.getMemberScope().getClassifier(segments.first()) as? ClassDescriptor ?: return null
    var result = topLevelClass
    for (name in segments.subList(1, segments.size())) {
        result = result.getUnsubstitutedInnerClassesScope().getClassifier(name) as? ClassDescriptor ?: return null
    }
    return result
}
