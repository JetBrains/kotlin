/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule

fun JavaModule.getJavaModuleRoots(): List<JavaRoot> =
    moduleRoots.map { (root, isBinary, isBinarySignature) ->
        val type = when {
            isBinarySignature -> JavaRoot.RootType.BINARY_SIG
            isBinary -> JavaRoot.RootType.BINARY
            else -> JavaRoot.RootType.SOURCE
        }
        JavaRoot(root, type)
    }

/**
 * Computes the JDK's default root modules. See [JEP 261: Module System](http://openjdk.java.net/jeps/261).
 */
fun CliJavaModuleFinder.computeDefaultRootModules(): List<String> {
    val result = arrayListOf<String>()

    val systemModules = systemModules.associateBy(JavaModule::name)
    val javaSeExists = "java.se" in systemModules
    if (javaSeExists) {
        // The java.se module is a root, if it exists.
        result.add("java.se")
    }

    fun JavaModule.Explicit.exportsAtLeastOnePackageUnqualified(): Boolean = moduleInfo.exports.any { it.toModules.isEmpty() }

    if (!javaSeExists) {
        // If it does not exist then every java.* module on the upgrade module path or among the system modules
        // that exports at least one package, without qualification, is a root.
        for ((name, module) in systemModules) {
            if (name.startsWith("java.") && module.exportsAtLeastOnePackageUnqualified()) {
                result.add(name)
            }
        }
    }

    for ((name, module) in systemModules) {
        // Every non-java.* module on the upgrade module path or among the system modules that exports at least one package,
        // without qualification, is also a root.
        if (!name.startsWith("java.") && module.exportsAtLeastOnePackageUnqualified()) {
            result.add(name)
        }
    }

    return result
}
