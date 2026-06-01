/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.generator

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import java.nio.file.Path
import kotlin.io.path.Path

internal class BtaApiVersionGenerator(
    private val kotlinVersion: KotlinReleaseVersion,
    private val targetPackage: String,
) {
    fun generate(): List<Pair<Path, String>> {
        val appendable = createGeneratedFileAppendable()
        val objectSpec = TypeSpec.objectBuilder("BuildToolsApiVersion")
            .addModifiers(KModifier.INTERNAL)
            .addKdoc("@since 2.4.20\n")
            .addFunction(
                FunSpec.builder("get")
                    .addAnnotation(JvmStatic::class)
                    .addModifiers(KModifier.INTERNAL)
                    .returns(String::class)
                    .addStatement("return %S", kotlinVersion.releaseName)
                    .build()
            ).build()
        val fileSpec = FileSpec.builder(targetPackage, "BuildToolsApiVersion")
            .addType(objectSpec)
            .build()

        fileSpec.writeTo(appendable)
        return [Path(fileSpec.relativePath) to appendable.toString()]
    }
}
