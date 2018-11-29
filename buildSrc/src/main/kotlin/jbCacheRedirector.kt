/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI

// https://youtrack.jetbrains.com/issue/ADM-23180
val mirroredUrls = listOf(
    "https://dl.bintray.com/groovy/maven",
    "https://dl.bintray.com/kotlin/kotlin-dev",
    "https://dl.bintray.com/kotlin/kotlin-eap",
    "https://dl.google.com/dl/android/maven2",
    "https://dl.google.com/go",
    "https://download.jetbrains.com",
    "https://jcenter.bintray.com",
    "https://jetbrains.bintray.com/dekaf",
    "https://jetbrains.bintray.com/intellij-jdk",
    "https://jetbrains.bintray.com/intellij-plugin-service",
    "https://jetbrains.bintray.com/intellij-third-party-dependencies",
    "https://jetbrains.bintray.com/markdown",
    "https://jetbrains.bintray.com/teamcity-rest-client",
    "https://jetbrains.bintray.com/test-discovery",
    "https://jetbrains.bintray.com/jediterm",
    "https://jitpack.io",
    "https://maven.exasol.com/artifactory/exasol-releases",
    "https://plugins.gradle.org/m2",
    "https://plugins.jetbrains.com/maven",
    "https://repo.grails.org/grails/core",
    "https://repo.jenkins-ci.org/releases",
    "https://repo.spring.io/milestone",
    "https://repo1.maven.org/maven2",
    "https://services.gradle.org",
    "https://www.jetbrains.com/intellij-repository",
    "https://www.myget.org/F/intellij-go-snapshots/maven",
    "https://www.myget.org/F/rd-snapshots/maven",
    "https://www.myget.org/F/rd-model-snapshots/maven",
    "https://www.python.org/ftp",
    "https://dl.google.com/dl/android/studio/ide-zips",
    "https://dl.bintray.com/kotlin/ktor",
    "https://cdn.azul.com/zulu/bin"
)

fun URI.toCacheRedirectorUri() = URI("https://cache-redirector.jetbrains.com/$host/$path")

fun RepositoryHandler.redirect() = filterIsInstance<MavenArtifactRepository>().forEach { repository ->
    val uri = repository.url
    if (uri.toString().trimEnd('/') in mirroredUrls) {
        repository.url = uri.toCacheRedirectorUri()
    }
}

fun Project.cacheRedirectorEnabled(): Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

fun RepositoryHandler.withRedirector(project: Project, configuration: RepositoryHandler.() -> Unit) {
    configuration()
    if (project.cacheRedirectorEnabled()) {
        redirect()
    }
}