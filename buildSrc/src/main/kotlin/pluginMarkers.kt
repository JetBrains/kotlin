/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.gradle.publish.PluginBundleExtension
import com.gradle.publish.PluginConfig
import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import plugins.KotlinBuildPublishingPlugin

internal const val PLUGIN_MARKER_SUFFIX = ".gradle.plugin"

fun Project.publishPluginMarkers(withEmptyJars: Boolean = true) {
    val pluginDevelopment = extensions.getByType<PluginBundleExtension>()
    val publishingExtension = extensions.getByType<PublishingExtension>()
    val mainPublication = publishingExtension.publications[KotlinBuildPublishingPlugin.PUBLICATION_NAME] as MavenPublication

    pluginDevelopment.plugins.forEach { declaration ->
        val markerPublication = createMavenMarkerPublication(declaration, mainPublication, publishingExtension.publications)
        if (withEmptyJars) {
            addEmptyJarArtifacts(markerPublication)
        }
    }
}

fun Project.addEmptyJarArtifacts(publication: MavenPublication) {
    val emptyJar = getOrCreateTask<Jar>("emptyJar") { }
    publication.artifact(emptyJar.get()) { }
    publication.artifact(emptyJar.get()) { classifier = "sources" }
    publication.artifact(emptyJar.get()) { classifier = "javadoc" }
}

// Based on code from `java-gradle-plugin`
// https://github.com/gradle/gradle/blob/v6.4.0/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L84
private fun createMavenMarkerPublication(
    declaration: PluginConfig,
    coordinates: MavenPublication,
    publications: PublicationContainer
): MavenPublication {
    return publications.create<MavenPublication>(declaration.name.toString() + "PluginMarkerMaven") {
        val pluginId: String = declaration.id
        artifactId = pluginId + PLUGIN_MARKER_SUFFIX
        groupId = pluginId
        pom.withXml {
            val root = asElement()
            val document = root.ownerDocument
            val dependencies = root.appendChild(document.createElement("dependencies"))
            val dependency = dependencies.appendChild(document.createElement("dependency"))
            val groupId = dependency.appendChild(document.createElement("groupId"))
            groupId.textContent = coordinates.groupId
            val artifactId = dependency.appendChild(document.createElement("artifactId"))
            artifactId.textContent = coordinates.artifactId
            val version = dependency.appendChild(document.createElement("version"))
            version.textContent = coordinates.version
        }

        pom.name.set(declaration.displayName)
        pom.description.set(declaration.description)
    }
}
