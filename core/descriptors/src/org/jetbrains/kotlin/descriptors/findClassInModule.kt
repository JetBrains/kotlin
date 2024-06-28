/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.getResolutionAnchorIfAny

fun ModuleDescriptor.findClassifierAcrossModuleDependencies(classId: ClassId): ClassifierDescriptor? = withResolutionAnchor {
    val packageViewDescriptor = getPackage(classId.packageFqName)
    val segments = classId.relativeClassName.pathSegments()
    val topLevelClass = packageViewDescriptor.memberScope.getContributedClassifier(
        segments.first(),
        NoLookupLocation.FROM_DESERIALIZATION
    ) ?: return@withResolutionAnchor null
    var result = topLevelClass
    for (name in segments.subList(1, segments.size)) {
        if (result !is ClassDescriptor) return@withResolutionAnchor null
        result = result.unsubstitutedInnerClassesScope
            .getContributedClassifier(name, NoLookupLocation.FROM_DESERIALIZATION) as? ClassDescriptor
            ?: return@withResolutionAnchor null
    }
    return@withResolutionAnchor result
}

private inline fun ModuleDescriptor.withResolutionAnchor(
    crossinline doSearch: ModuleDescriptor.() -> ClassifierDescriptor?
): ClassifierDescriptor? {
    val anchor = getResolutionAnchorIfAny()
    return if (anchor == null) doSearch() else anchor.doSearch() ?: doSearch()
}

fun ModuleDescriptor.findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? =
    findClassifierAcrossModuleDependencies(classId) as? ClassDescriptor

// Returns a mock class descriptor if no existing class is found.
// NB: the returned class has no type parameters and thus cannot be given arguments in types
fun ModuleDescriptor.findNonGenericClassAcrossDependencies(classId: ClassId, notFoundClasses: NotFoundClasses): ClassDescriptor {
    val existingClass = findClassAcrossModuleDependencies(classId)
    if (existingClass != null) return existingClass

    // Take a list of N zeros, where N is the number of class names in the given ClassId
    val typeParametersCount = generateSequence(classId, ClassId::outerClassId).map { 0 }.toList()

    return notFoundClasses.getClass(classId, typeParametersCount)
}

fun ModuleDescriptor.findTypeAliasAcrossModuleDependencies(classId: ClassId): TypeAliasDescriptor? {
    // TODO what if typealias becomes a class / interface?
    return findClassifierAcrossModuleDependencies(classId) as? TypeAliasDescriptor
}
