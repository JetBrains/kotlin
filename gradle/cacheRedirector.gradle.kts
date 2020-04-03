/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI

/**
 *  The list of repositories supported by cache redirector should be synced with the list at https://cache-redirector.jetbrains.com/redirects_generated.html
 *  To add a repository to the list create an issue in ADM project (example issue https://youtrack.jetbrains.com/issue/IJI-149)
 *  Repositories in `buildscript` blocks are *NOT* substituted by this script and should be handled manually
 */
val mirroredUrls = listOf(
    "https://cdn.azul.com/zulu/bin",
    "https://clojars.org/repo",
    "https://dl.bintray.com/d10xa/maven",
    "https://dl.bintray.com/groovy/maven",
    "https://dl.bintray.com/jetbrains/maven-patched",
    "https://dl.bintray.com/jetbrains/scala-plugin-deps",
    "https://dl.bintray.com/kodein-framework/Kodein-DI",
    "https://dl.bintray.com/konsoletyper/teavm",
    "https://dl.bintray.com/kotlin/kotlin-dev",
    "https://dl.bintray.com/kotlin/kotlin-eap",
    "https://dl.bintray.com/kotlin/kotlinx.html",
    "https://dl.bintray.com/kotlin/kotlinx",
    "https://dl.bintray.com/kotlin/ktor",
    "https://dl.bintray.com/scalacenter/releases",
    "https://dl.bintray.com/scalamacros/maven",
    "https://dl.bintray.com/kotlin/exposed",
    "https://dl.bintray.com/cy6ergn0m/maven",
    "https://dl.bintray.com/kotlin/kotlin-js-wrappers",
    "https://dl.google.com/android/repository",
    "https://dl.google.com/dl/android/maven2",
    "https://dl.google.com/dl/android/studio/ide-zips",
    "https://dl.google.com/go",
    "https://download.jetbrains.com",
    "https://jcenter.bintray.com",
    "https://jetbrains.bintray.com/dekaf",
    "https://jetbrains.bintray.com/intellij-jbr",
    "https://jetbrains.bintray.com/intellij-jdk",
    "https://jetbrains.bintray.com/intellij-plugin-service",
    "https://jetbrains.bintray.com/intellij-terraform",
    "https://jetbrains.bintray.com/intellij-third-party-dependencies",
    "https://jetbrains.bintray.com/jediterm",
    "https://jetbrains.bintray.com/kotlin-native-dependencies",
    "https://jetbrains.bintray.com/markdown",
    "https://jetbrains.bintray.com/teamcity-rest-client",
    "https://jetbrains.bintray.com/test-discovery",
    "https://jetbrains.bintray.com/wormhole",
    "https://jitpack.io",
    "https://kotlin.bintray.com/dukat",
    "https://kotlin.bintray.com/kotlin-dependencies",
    "https://oss.sonatype.org/content/repositories/releases",
    "https://oss.sonatype.org/content/repositories/snapshots",
    "https://oss.sonatype.org/content/repositories/staging",
    "https://packages.confluent.io/maven/",
    "https://plugins.gradle.org/m2",
    "https://plugins.jetbrains.com/maven",
    "https://repo1.maven.org/maven2",
    "https://repo.grails.org/grails/core",
    "https://repo.jenkins-ci.org/releases",
    "https://repo.maven.apache.org/maven2",
    "https://repo.spring.io/milestone",
    "https://repo.typesafe.com/typesafe/ivy-releases",
    "https://services.gradle.org",
    "https://www.exasol.com/artifactory/exasol-releases",
    "https://www.myget.org/F/intellij-go-snapshots/maven",
    "https://www.myget.org/F/rd-model-snapshots/maven",
    "https://www.myget.org/F/rd-snapshots/maven",
    "https://www.python.org/ftp",
    "https://www.jetbrains.com/intellij-repository/nightly",
    "https://www.jetbrains.com/intellij-repository/releases",
    "https://www.jetbrains.com/intellij-repository/snapshots"
)

val aliases = mapOf(
    "https://repo.maven.apache.org/maven2" to "https://repo1.maven.org/maven2" // Maven Central
)

val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null

fun Project.cacheRedirectorEnabled(): Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

fun URI.toCacheRedirectorUri() = URI("https://cache-redirector.jetbrains.com/$host/$path")

fun URI.maybeRedirect(): URI {
    val url = toString().trimEnd('/')
    val dealiasedUrl = aliases.getOrDefault(url, url)

    return if (mirroredUrls.any { dealiasedUrl.startsWith(it) }) {
        URI(dealiasedUrl).toCacheRedirectorUri()
    } else {
        this
    }
}

fun RepositoryHandler.redirect() {
    for (repository in this) {
        when (repository) {
            is MavenArtifactRepository -> repository.url = repository.url.maybeRedirect()
            is IvyArtifactRepository -> if (repository.url != null) {
                repository.url = repository.url.maybeRedirect()
            }
        }
    }
}

fun URI.isCachedOrLocal() = scheme == "file" ||
            host == "cache-redirector.jetbrains.com" ||
            host == "teamcity.jetbrains.com" ||
            host == "buildserver.labs.intellij.net"

fun RepositoryHandler.findNonCachedRepositories(): List<String> {
    val mavenNonCachedRepos = filterIsInstance<MavenArtifactRepository>()
        .filterNot { it.url.isCachedOrLocal() }
        .map { it.url.toString() }

    val ivyNonCachedRepos = filterIsInstance<IvyArtifactRepository>()
        .filterNot { it.url.isCachedOrLocal() }
        .map { it.url.toString() }

    return mavenNonCachedRepos + ivyNonCachedRepos
}

fun escape(s: String): String {
    return s.replace("[\\|'\\[\\]]".toRegex(), "\\|$0").replace("\n".toRegex(), "|n").replace("\r".toRegex(), "|r")
}

fun testStarted(testName: String) {
    println("##teamcity[testStarted name='%s']".format(escape(testName)))
}

fun testFinished(testName: String) {
    println("##teamcity[testFinished name='%s']".format(escape(testName)))
}

fun testFailed(name: String, message: String, details: String) {
    println("##teamcity[testFailed name='%s' message='%s' details='%s']".format(escape(name), escape(message), escape(details)))
}

fun Task.logNonCachedRepo(testName: String, repoUrl: String) {
    val msg = "Repository $repoUrl in ${project.displayName} should be cached with cache-redirector"
    val details = "Using non cached repository may lead to download failures in CI builds." +
            " Check https://github.com/JetBrains/kotlin/blob/master/gradle/cacheRedirector.gradle.kts for details."

    if (isTeamcityBuild) {
        testFailed(testName, msg, details)
    }

    logger.warn("WARNING - $msg\n$details")
}

fun Task.logInvalidIvyRepo(testName: String) {
    val msg = "Invalid ivy repo found in ${project.displayName}"
    val details = "Url must be not null"

    if (isTeamcityBuild) {
        testFailed(testName, msg, details)
    }

    logger.warn("WARNING - $msg: $details")
}

val checkRepositories: TaskProvider<Task> = tasks.register("checkRepositories") {
    doLast {
        val testName = "$name in ${project.displayName}"
        if (isTeamcityBuild) {
            testStarted(testName)
        }

        project.repositories.filterIsInstance<IvyArtifactRepository>().forEach {
            if (it.url == null) {
                logInvalidIvyRepo(testName)
            }
        }

        project.repositories.findNonCachedRepositories().forEach {
            logNonCachedRepo(testName, it)
        }

        project.buildscript.repositories.findNonCachedRepositories().forEach {
            logNonCachedRepo(testName, it)
        }

        if (isTeamcityBuild) {
            testFinished(testName)
        }
    }
}

tasks.named("checkBuild").configure {
    dependsOn(checkRepositories)
}

if (cacheRedirectorEnabled()) {
    logger.info("Redirecting repositories for $displayName")
    repositories.redirect()
}