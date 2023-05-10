import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.spdx.sbom.gradle.SpdxSbomExtension
import org.spdx.sbom.gradle.SpdxSbomPlugin
import java.util.*

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Project.configureSbom(
    moduleName: String = this.name,
    gradleConfigurations: Iterable<String> = setOf("runtimeClasspath"),
): Configuration {
    val project = this
    apply<SpdxSbomPlugin>()

    configure<SpdxSbomExtension> {
        targets.create(moduleName) {
            configurations.set(gradleConfigurations)
            scm {
                tool.set("git")
                uri.set("https://github.com/JetBrains/kotlin.git")
                revision.set("v${project.version}")
            }

            // adjust properties of the document
            document {
                this.name.set("SpdxDoc for $moduleName")
                // NOTE: The URI does not have to be accessible. It is only intended to provide a unique ID.
                // In many cases, the URI will point to a Web accessible document, but this should not be assumed to be the case.
                namespace.set("https://www.jetbrains.com/spdxdocs/${UUID.randomUUID()}")
                creator.set("Organization: JetBrains s.r.o.")
                packageSupplier.set("Organization: JetBrains s.r.o.")
            }
        }
    }

    val sbomCfg = configurations.maybeCreate("sbomFor$moduleName").apply {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    val sbomFile = layout.buildDirectory.file("spdx/$moduleName.spdx.json")
    artifacts.add(sbomCfg.name, sbomFile.get().asFile) {
        type = "sbom"
        extension = "spdx.json"
        val capitalizedModuleName =
            moduleName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        builtBy("spdxSbomFor$capitalizedModuleName")
    }
    return sbomCfg
}
