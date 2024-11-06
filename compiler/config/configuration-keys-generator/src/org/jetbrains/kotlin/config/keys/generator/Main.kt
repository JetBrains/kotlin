/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainerGenerator
import java.io.File

val allContainers: List<KeysContainer> = listOf(
    CommonConfigurationKeysContainer,
    CLIConfigurationKeysContainer,
    KlibConfigurationKeysContainer,
    JvmConfigurationKeysContainer,
    JsConfigurationKeysContainer,
)

fun main(args: Array<String>) {
    require(args.size >= 2) {
        "Usage: <output dir> [<ConfigurationKeysName> ...]"
    }
    val dirPath = args[0]
    val dir = File(dirPath)
    val containers = args.drop(1).map { name ->
        allContainers.find { it.className == name }
            ?: error("Container with name $name not found. Available classes: ${allContainers.map { it.className }}")
    }

    for (container in containers) {
        var file = dir
        for (packageSegment in container.packageName.split(".")) {
            file = file.resolve(packageSegment)
        }
        file = file.resolve(container.className + ".kt")
        KeysContainerGenerator.generate(file, container)
    }
}
