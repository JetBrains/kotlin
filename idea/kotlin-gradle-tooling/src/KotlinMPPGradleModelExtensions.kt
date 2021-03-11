/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

@ExperimentalGradleToolingApi
fun KotlinMPPGradleModel.getCompilations(sourceSet: KotlinSourceSet): Set<KotlinCompilation> {
    return targets.flatMap { target -> target.compilations }
        .filter { compilation -> compilationDependsOnSourceSet(compilation, sourceSet) }
        .toSet()
}

@ExperimentalGradleToolingApi
fun KotlinMPPGradleModel.compilationDependsOnSourceSet(
    compilation: KotlinCompilation, sourceSet: KotlinSourceSet
): Boolean {
    return compilation.declaredSourceSets.any { sourceSetInCompilation ->
        sourceSetInCompilation == sourceSet || sourceSetInCompilation.isDependsOn(this, sourceSet)
    }
}
