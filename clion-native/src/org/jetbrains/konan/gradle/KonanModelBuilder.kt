/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
import org.jetbrains.kotlin.gradle.KotlinCompilation
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.gradle.getMethodOrNull
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.File
import java.lang.Exception

class KonanModelBuilder : ModelBuilderService {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder
            .create(project, e, "Gradle import errors")
            .withDescription("Unable to build Konan project configuration")
    }

    override fun canBuild(modelName: String?): Boolean {
        return modelName == KonanModel::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        println("build")


        val artifacts = project.tasks.mapNotNull { task ->
            buildArtifact(task)
        }
        return KonanModelImpl(artifacts)
    }

    private fun buildArtifact(task: Task): KonanModelArtifact? {
        val outputKind = task["getOutputKind"]["name"] as? String ?: return null
        println("outputKind:${outputKind}")
        val konanTargetName = task["getTarget"] as? String ?: error("No arch target found")
        println("konanTargetName:${konanTargetName}")
        val outputFile = (task["getOutputFile"] as? Provider<*>)?.orNull as? File ?: return null
        println("outputFile:${outputFile}")
        val compilationTargetName = task["getCompilation"]["getTarget"]["getName"] as? String ?: return null
        println("compilationTargetName:${compilationTargetName}")

        return KonanModelArtifactImpl(compilationTargetName, CompilerOutputKind.valueOf(outputKind), konanTargetName, outputFile, task.name).also {
            println(it)
        }

    }

    private operator fun Any?.get(methodName: String): Any? {
        if (this == null) return null
        return this::class.java.getMethodOrNull(methodName)?.invoke(this)
    }

}