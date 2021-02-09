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
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import plugins.KotlinBuildPublishingPlugin
import plugins.configureRepository
import java.util.*

internal const val PLUGIN_MARKER_SUFFIX = ".gradle.plugin"

@OptIn(ExperimentalStdlibApi::class)
fun Project.publishPluginMarkers(withEmptyJars: Boolean = true) {

    fun Project.isSonatypePublish(): Boolean =
        hasProperty("isSonatypePublish") && property("isSonatypePublish") as Boolean

    val pluginDevelopment = extensions.getByType<PluginBundleExtension>()
    val publishingExtension = extensions.getByType<PublishingExtension>()
    val mainPublication = publishingExtension.publications[KotlinBuildPublishingPlugin.PUBLICATION_NAME] as MavenPublication

    pluginDevelopment.plugins.forEach { declaration ->
        val markerPublication = createMavenMarkerPublication(declaration, mainPublication, publishingExtension.publications)
        if (withEmptyJars) {
            addEmptyJarArtifacts(markerPublication)
        }

        tasks.named<PublishToMavenRepository>(
            "publish${markerPublication.name.capitalize(Locale.ROOT)}PublicationTo${KotlinBuildPublishingPlugin.REPOSITORY_NAME}Repository"
        ).apply {
            configureRepository()
            configure {
                onlyIf { !isSonatypePublish() }
            }
        }
    }
}

fun Project.addEmptyJarArtifacts(publication: MavenPublication) {
    val emptyJar = getOrCreateTask<Jar>("emptyJar") {
        archiveBaseName.set("empty")
    }

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
        val cGroupId = coordinates.groupId
        val cArtifactId = coordinates.artifactId
        val cVersion = coordinates.version
        artifactId = pluginId + PLUGIN_MARKER_SUFFIX
        groupId = pluginId
        pom.withXml {
            val root = asElement()
            val document = root.ownerDocument
            val dependencies = root.appendChild(document.createElement("dependencies"))
            val dependency = dependencies.appendChild(document.createElement("dependency"))
            val groupId = dependency.appendChild(document.createElement("groupId"))
            groupId.textContent = cGroupId
            val artifactId = dependency.appendChild(document.createElement("artifactId"))
            artifactId.textContent = cArtifactId
            val version = dependency.appendChild(document.createElement("version"))
            version.textContent = cVersion
        }

        pom.name.set(declaration.displayName)
        pom.description.set(declaration.description)
    }
}
