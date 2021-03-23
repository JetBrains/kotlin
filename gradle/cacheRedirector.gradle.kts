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
val cacheMap: Map<String, String> = mapOf(
    "https://jetbrains.bintray.com/xodus" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/xodus",
    "https://cdn.azul.com/zulu/bin" to "https://cache-redirector.jetbrains.com/cdn.azul.com/zulu/bin",
    "https://clojars.org/repo" to "https://cache-redirector.jetbrains.com/clojars.org/repo",
    "https://downloads.apache.org" to "https://cache-redirector.jetbrains.com/downloads.apache.org",
    "https://download.eclipse.org" to "https://cache-redirector.jetbrains.com/download.eclipse.org",
    "https://downloads.gradle.org" to "https://cache-redirector.jetbrains.com/downloads.gradle.org",
    "https://dl.bintray.com/d10xa/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com/d10xa/maven",
    "https://dl.bintray.com/jetbrains/golang" to "https://cache-redirector.jetbrains.com/dl.bintray.com/jetbrains/golang",
    "https://dl.bintray.com/groovy/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com.groovy",
    "https://dl.bintray.com/jetbrains/maven-patched" to "https://cache-redirector.jetbrains.com/dl.bintray.com/jetbrains/maven-patched",
    "https://dl.bintray.com/jetbrains/scala-plugin-deps" to "https://cache-redirector.jetbrains.com/dl.bintray.com/jetbrains/scala-plugin-deps",
    "https://dl.bintray.com/kodein-framework/Kodein-DI" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kodein-framework/Kodein-DI",
    "https://dl.bintray.com/konsoletyper/teavm" to "https://cache-redirector.jetbrains.com/dl.bintray.com/konsoletyper/teavm",
    "https://dl.bintray.com/kotlin/kotlin-bootstrap" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-bootstrap",
    "https://dl.bintray.com/kotlin/kotlin-dev" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev",
    "https://dl.bintray.com/kotlin/kotlin-eap" to "https://cache-redirector.jetbrains.com/kotlin-eap",
    "https://dl.bintray.com/kotlin/kotlinx.html" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx.html",
    "https://dl.bintray.com/kotlin/kotlinx" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx",
    "https://dl.bintray.com/kotlin/ktor" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/ktor",
    "https://dl.bintray.com/scalacenter/releases" to "https://cache-redirector.jetbrains.com/dl.bintray.com/scalacenter/releases",
    "https://dl.bintray.com/scalamacros/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com/scalamacros/maven",
    "https://dl.bintray.com/kotlin/exposed" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/exposed",
    "https://dl.bintray.com/cy6ergn0m/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com/cy6ergn0m/maven",
    "https://dl.bintray.com/kotlin/kotlin-js-wrappers" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-js-wrappers",
    "https://dl.google.com/android/repository" to "https://cache-redirector.jetbrains.com/dl.google.com/android/repository",
    "https://dl.google.com/dl/android/maven2" to "https://cache-redirector.jetbrains.com/dl.google.com.android.maven2",
    "https://dl.google.com/dl/android/studio/ide-zips" to "https://cache-redirector.jetbrains.com/dl.google.com/dl/android/studio/ide-zips",
    "https://dl.google.com/go" to "https://cache-redirector.jetbrains.com/dl.google.com.go",
    "https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download" to "https://cache-redirector.jetbrains.com/github.com/AdoptOpenJDK/openjdk14-binaries/releases/download",
    "https://jcenter.bintray.com" to "https://cache-redirector.jetbrains.com/jcenter",
    "https://jetbrains.bintray.com/dekaf" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com.dekaf",
    "https://jetbrains.bintray.com/intellij-jbr" to "https://cache-redirector.jetbrains.com/intellij-jbr",
    "https://jetbrains.bintray.com/intellij-jdk" to "https://cache-redirector.jetbrains.com/intellij-jdk",
    "https://jetbrains.bintray.com/intellij-plugin-service" to "https://cache-redirector.jetbrains.com/intellij-plugin-service",
    "https://jetbrains.bintray.com/intellij-terraform" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-terraform",
    "https://jetbrains.bintray.com/intellij-shared-indexes" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-shared-indexes",
    "https://jetbrains.bintray.com/intellij-third-party-dependencies" to "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies",
    "https://jetbrains.bintray.com/jediterm" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/jediterm",
    "https://jetbrains.bintray.com/kotlin-native-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/kotlin-native-dependencies",
    "https://jetbrains.bintray.com/markdown" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com.markdown",
    "https://jetbrains.bintray.com/teamcity-rest-client" to "https://cache-redirector.jetbrains.com/teamcity-rest-client",
    "https://jetbrains.bintray.com/test-discovery" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/test-discovery",
    "https://jetbrains.bintray.com/wormhole" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/wormhole",
    "https://jetbrains.bintray.com/space" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/space",
    "https://jetbrains.bintray.com/rplugin" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/rplugin",
    "https://jitpack.io" to "https://cache-redirector.jetbrains.com/jitpack.io",
    "https://maven.fabric.io/public" to "https://cache-redirector.jetbrains.com/maven.fabric.io/public",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/eap" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/eap",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies",
    "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-plugin" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-plugin",
    "https://maven.pkg.jetbrains.space/public/p/space/maven" to "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/public/p/space/maven",
    "https://nodejs.org/dist" to "https://cache-redirector.jetbrains.com/nodejs.org/dist",
    "https://kotlin.bintray.com/dukat" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/dukat",
    "https://kotlin.bintray.com/kotlin-dependencies" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-dependencies",
    "https://kotlin.bintray.com/kotlin-plugin" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-plugin",
    "https://kotlin.bintray.com/kotlin-ide-plugin-dependencies" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-ide-plugin-dependencies",
    "https://oss.sonatype.org/content/repositories/releases" to "https://cache-redirector.jetbrains.com/oss.sonatype.org/content/repositories/releases",
    "https://oss.sonatype.org/content/repositories/snapshots" to "https://cache-redirector.jetbrains.com/oss.sonatype.org/content/repositories/snapshots",
    "https://oss.sonatype.org/content/repositories/staging" to "https://cache-redirector.jetbrains.com/oss.sonatype.org/content/repositories/staging",
    "https://packages.confluent.io/maven/" to "https://cache-redirector.jetbrains.com/packages.confluent.io/maven/",
    "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies" to "https://cache-redirector.jetbrains.com/intellij-dependencies",
    "https://packages.jetbrains.team/maven/p/skija/maven" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/skija/maven",
    "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org",
    "https://plugins.jetbrains.com/maven" to "https://cache-redirector.jetbrains.com/plugins.jetbrains.com/maven",
    "https://repo1.maven.org/maven2" to "https://cache-redirector.jetbrains.com/maven-central",
    "https://repo.eclipse.org/content/repositories/egit-releases" to "https://cache-redirector.jetbrains.com/repo.eclipse.org/content/repositories/egit-releases",
    "https://repo.grails.org/grails/core" to "https://cache-redirector.jetbrains.com/repo.grails.org/grails/core",
    "https://repo.jenkins-ci.org/releases" to "https://cache-redirector.jetbrains.com/repo.jenkins-ci.org.releases",
    "https://repo.maven.apache.org/maven2" to "https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2",
    "https://repo.spring.io/milestone" to "https://cache-redirector.jetbrains.com/repo.spring.io.milestone",
    "https://repo.typesafe.com/typesafe/ivy-releases" to "https://cache-redirector.jetbrains.com/repo.typesafe.com/typesafe/ivy-releases",
    "https://repository.jboss.org/nexus/content/repositories/public" to "https://cache-redirector.jetbrains.com/repository.jboss.org/nexus/content/repositories/public",
    "https://services.gradle.org" to "https://cache-redirector.jetbrains.com/services.gradle.org",
    "https://maven.exasol.com/artifactory/exasol-releases" to "https://cache-redirector.jetbrains.com/maven.exasol.com.releases",
    "https://www.myget.org/F/intellij-go-snapshots/maven" to "https://cache-redirector.jetbrains.com/myget.org.intellij-go-snapshots",
    "https://www.myget.org/F/rd-model-snapshots/maven" to "https://cache-redirector.jetbrains.com/www.myget.org/F/rd-model-snapshots/maven",
    "https://www.myget.org/F/rd-snapshots/maven" to "https://cache-redirector.jetbrains.com/myget.org.rd-snapshots.maven",
    "https://www.python.org/ftp" to "https://cache-redirector.jetbrains.com/www.python.org/ftp",
    "https://www.jetbrains.com/intellij-repository/nightly" to "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/nightly",
    "https://www.jetbrains.com/jps-cache/intellij" to "https://cache-redirector.jetbrains.com/www.jetbrains.com/jps-cache/intellij",
    "https://www.jetbrains.com/intellij-repository/releases" to "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/releases",
    "https://www.jetbrains.com/intellij-repository/snapshots" to "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/snapshots",
    "https://download.jetbrains.com" to "https://cache-redirector.jetbrains.com/download.jetbrains.com",
    "https://secure.index-cdn.jetbrains.com" to "https://cache-redirector.jetbrains.com/secure.index-cdn.jetbrains.com",
    "https://maven.apache.org/xsd" to "https://cache-redirector.jetbrains.com/maven.apache.org/xsd"
)

val aliases = mapOf(
    "https://repo.maven.apache.org/maven2" to "https://repo1.maven.org/maven2" // Maven Central
)

val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null

fun Project.cacheRedirectorEnabled(): Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

fun URI.maybeRedirect(): URI {
    val url = toString().trimEnd('/')
    val dealiasedUrl = aliases.getOrDefault(url, url)

    val cacheUrlEntry = cacheMap.entries.find { (origin, _) -> dealiasedUrl.startsWith(origin) }
    return if (cacheUrlEntry != null) {
        val cacheUrl = cacheUrlEntry.value
        val originRestPath = dealiasedUrl.substringAfter(cacheUrlEntry.key, "")
        URI("$cacheUrl$originRestPath")
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

afterEvaluate {
    if (cacheRedirectorEnabled()) {
        logger.info("Redirecting repositories for $displayName")
        repositories.redirect()
    }
}