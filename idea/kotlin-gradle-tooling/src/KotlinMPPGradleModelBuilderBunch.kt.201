/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.plugins.gradle.model.DefaultExternalProjectDependency

//BUNCH: 193
fun makeConfigurationNamesDefault(dependencyMapper: KotlinDependencyMapper) {
    dependencyMapper.toDependencyMap().values.forEach {
        (it as? DefaultExternalProjectDependency)?.configurationName = "default"
    }
}
