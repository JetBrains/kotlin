import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.spdx.sbom.gradle.SpdxSbomExtension
import org.spdx.sbom.gradle.SpdxSbomPlugin
import plugins.mainPublicationName
import java.util.*

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Project.configureSbom(gradleConfigurations: Iterable<String> = setOf("runtimeClasspath")) {
    val project = this
    apply<SpdxSbomPlugin>()

    configure<SpdxSbomExtension> {
        targets.create(project.name) {
            configurations.set(gradleConfigurations)
            scm {
                tool.set("git")
                uri.set("https://github.com/JetBrains/kotlin.git")
                revision.set("v${project.version}")
            }

            // adjust properties of the document
            document {
                name.set("SpdxDoc for ${project.name}")
                // NOTE: The URI does not have to be accessible. It is only intended to provide a unique ID.
                // In many cases, the URI will point to a Web accessible document, but this should not be assumed to be the case.
                namespace.set("https://www.jetbrains.com/spdxdocs/${UUID.randomUUID()}")
                creator.set("Organization: JetBrains s.r.o.")
                packageSupplier.set("Organization: JetBrains s.r.o.")
            }
        }
    }

    configurations.maybeCreate("sbom")
    val sbomFile = layout.buildDirectory.file("spdx/${project.name}.spdx.json")
    val sbomArtifact = artifacts.add("sbom", sbomFile.get().asFile) {
        type = "sbom"
        extension = "spdx.json"
        builtBy("spdxSbom")
    }
    val publication = extensions.findByType<PublishingExtension>()
        ?.publications
        ?.findByName(mainPublicationName) as MavenPublication?
    publication?.apply {
        artifact(sbomArtifact)
    }
}
