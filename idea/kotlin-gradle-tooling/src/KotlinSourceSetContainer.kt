/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

interface KotlinSourceSetContainer {
    val sourceSetsByName: Map<String, KotlinSourceSet>
}

val KotlinSourceSetContainer.sourceSets: List<KotlinSourceSet> get() = sourceSetsByName.values.toList()

fun KotlinSourceSetContainer.resolveDeclaredDependsOnSourceSets(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
    return sourceSet.declaredDependsOnSourceSets.mapNotNull { name -> sourceSetsByName[name] }.toSet()
}

fun KotlinSourceSetContainer.resolveAllDependsOnSourceSets(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
    /* Fast path */
    if (sourceSet.declaredDependsOnSourceSets.isEmpty()) return emptySet()

    /* Aggregating set containing all currently resolved source sets */
    val resolvedSourceSets = mutableSetOf<KotlinSourceSet>()

    /* Queue of source set names that shall be resolved */
    val declaredDependsOnSourceSetsQueue = ArrayDeque<String>()

    declaredDependsOnSourceSetsQueue.addAll(sourceSet.declaredDependsOnSourceSets)
    while (declaredDependsOnSourceSetsQueue.isNotEmpty()) {
        val sourceSetName = declaredDependsOnSourceSetsQueue.removeFirst()
        val resolvedSourceSet = sourceSetsByName[sourceSetName]
        if (resolvedSourceSet != null) {
            if (resolvedSourceSets.add(resolvedSourceSet)) {
                declaredDependsOnSourceSetsQueue.addAll(resolvedSourceSet.declaredDependsOnSourceSets)
            }
        }
    }

    return resolvedSourceSets
}

fun KotlinSourceSetContainer.isDependsOn(from: KotlinSourceSet, to: KotlinSourceSet): Boolean {
    return to in resolveAllDependsOnSourceSets(from)
}

fun KotlinSourceSet.isDependsOn(model: KotlinSourceSetContainer, sourceSet: KotlinSourceSet): Boolean {
    return model.isDependsOn(from = this, to = sourceSet)
}
