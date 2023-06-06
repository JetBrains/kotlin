/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.spdx.sbom.gradle.SpdxSbomExtension
import org.spdx.sbom.gradle.SpdxSbomPlugin
import org.spdx.sbom.gradle.SpdxSbomTask
import plugins.mainPublicationName
import java.util.*


fun Project.configureSbom(
    target: String? = null,
    documentName: String? = null,
    gradleConfigurations: Iterable<String> = setOf("runtimeClasspath"),
    publication: NamedDomainObjectProvider<MavenPublication>? = null
): TaskProvider<SpdxSbomTask> {
    val project = this
    val targetName = target ?: "${mainPublicationName}Publication"
    apply<SpdxSbomPlugin>()

    configure<SpdxSbomExtension> {
        targets.create(targetName) {
            configurations.set(gradleConfigurations)
            scm {
                tool.set("git")
                uri.set("https://github.com/JetBrains/kotlin.git")
                revision.set("v${project.version}")
            }

            // adjust properties of the document
            document {
                name.set(documentName ?: project.name)
                // NOTE: The URI does not have to be accessible. It is only intended to provide a unique ID.
                // In many cases, the URI will point to a Web accessible document, but this should not be assumed to be the case.
                namespace.set("https://www.jetbrains.com/spdxdocs/${UUID.randomUUID()}")
                creator.set("Organization: JetBrains s.r.o.")
                packageSupplier.set("Organization: JetBrains s.r.o.")
            }
        }
    }

    val spdxSbomTask = tasks.named<SpdxSbomTask>("spdxSbomFor$targetName")
    val sbomFile = layout.buildDirectory.file("spdx/$targetName.spdx.json")
    val sbomCfg = configurations.maybeCreate("sbomFor$targetName").apply {
        isCanBeResolved = false
        isCanBeConsumed = true
    }

    val sbomArtifact = artifacts.add(sbomCfg.name, sbomFile) {
        type = "sbom"
        extension = "spdx.json"
        builtBy(spdxSbomTask)
    }

    if (target == null) {
        pluginManager.withPlugin("kotlin-build-publishing") {
            val mainPublication = the<PublishingExtension>().publications[mainPublicationName] as MavenPublication
            mainPublication.artifact(sbomArtifact)
        }
    } else if (publication != null) {
        pluginManager.withPlugin("kotlin-build-publishing") {
            publication.configure {
                artifact(sbomArtifact)
            }
        }
    }

    return spdxSbomTask
}
