/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.getKlibDependencies
import java.io.File

fun getTransitivesAndFriendsPaths(module: TestModule, testServices: TestServices): List<String> {
    return getTransitivesAndFriends(module, testServices).toList().flatten().map { it.path }
}

fun getTransitivesAndFriends(module: TestModule, testServices: TestServices): Pair<List<File>, List<File>> {
    val transitiveLibraries = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
    val friendLibraries = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
    return transitiveLibraries to friendLibraries
}
