/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.descriptors.serialization

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.name.SpecialNames
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor

private fun findInnerClass(classDescriptor: ClassDescriptor, name: Name): ClassDescriptor? {
    return if (SpecialNames.isClassObjectName(name)) {
        classDescriptor.getClassObjectDescriptor()
    }
    else {
        classDescriptor.getUnsubstitutedInnerClassesScope().getClassifier(name) as? ClassDescriptor
    }
}

public fun ModuleDescriptor.findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? {
    val packageViewDescriptor = getPackage(classId.getPackageFqName()) ?: return null
    val segments = classId.getRelativeClassName().pathSegments()
    val topLevelClass = packageViewDescriptor.getMemberScope().getClassifier(segments.first!!) as? ClassDescriptor ?: return null
    var result = topLevelClass
    for (name in segments.subList(1, segments.size())) {
        result = findInnerClass(result, name) ?: return null
    }
    return result
}