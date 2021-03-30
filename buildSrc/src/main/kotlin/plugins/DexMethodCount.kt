/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.jakewharton.dex.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import java.io.File

@CacheableTask
abstract class DexMethodCount : DefaultTask() {

    data class Counts(
        val total: Int,
        val totalOwnPackages: Int?,
        val totalOtherPackages: Int?,
        val byPackage: Map<String, Int>,
        val byClass: Map<String, Int>
    )

    @Classpath
    lateinit var jarFile: File

    @get:Optional
    @get:Input
    abstract val ownPackages: ListProperty<String>

    @Internal
    var artifactName: String? = null

    private val projectName = project.name

    @get:Input
    val artifactOrArchiveName: String
        get() = artifactName ?: projectName

    fun from(jar: Jar) {
        jarFile = jar.archiveFile.get().asFile
        artifactName = jar.archiveBaseName.orNull
        dependsOn(jar)
    }

    @Internal // plain output properties are not supported, mark as internal to suppress warning from validatePlugins
    lateinit var counts: Counts

    @get:OutputFile
    val detailOutputFile: File by lazy {
        project.buildDir.resolve("$artifactOrArchiveName-method-count.txt")
    }

    @TaskAction
    fun invoke() {
        val methods = dexMethods(jarFile)
        val counts = methods.getCounts().also { this.counts = it }
        outputDetails(counts)
    }

    private fun List<DexMethod>.getCounts(): Counts {
        val byPackage = this.groupingBy { it.`package` }.eachCount()
        val byClass = this.groupingBy { it.declaringType }.eachCount()

        val ownPackages = ownPackages.map { list -> list.map { "$it." } }
        val byOwnPackages = if (ownPackages.isPresent) {
            this.partition { method -> ownPackages.get().any { method.declaringType.startsWith(it) } }.let {
                it.first.size to it.second.size
            }
        } else (null to null)

        return Counts(
            total = this.size,
            totalOwnPackages = byOwnPackages.first,
            totalOtherPackages = byOwnPackages.second,
            byPackage = byPackage,
            byClass = byClass
        )
    }

    private fun outputDetails(counts: Counts) {
        detailOutputFile.printWriter().use { writer ->
            writer.println("${counts.total.padRight()}\tTotal methods")
            ownPackages.orNull?.let { packages ->
                writer.println("${counts.totalOwnPackages?.padRight()}\tTotal methods from packages ${packages.joinToString { "$it.*" }}")
                writer.println("${counts.totalOtherPackages?.padRight()}\tTotal methods from other packages")
            }
            writer.println()
            writer.println("Method count by package:")
            counts.byPackage.forEach { (name, count) ->
                writer.println("${count.padRight()}\t$name")
            }
            writer.println()
            writer.println("Method count by class:")
            counts.byClass.forEach { (name, count) ->
                writer.println("${count.padRight()}\t$name")
            }
        }
    }
}

abstract class DexMethodCountStats : DefaultTask() {
    @get:InputFile
    internal abstract val inputFile: RegularFileProperty

    @get:Input
    internal abstract val artifactOrArchiveName: Property<String>

    @get:Input
    @get:Optional
    internal abstract val ownPackages: ListProperty<String>

    @TaskAction
    private fun printStats() {
        val artifactOrArchiveName = artifactOrArchiveName.get()
        inputFile.get().asFile.reader().useLines { lines ->
            fun String.getStatValue() = substringBefore("\t").trim()

            val statsLineCount = if (!ownPackages.isPresent) 1 else 3
            val stats = lines.take(statsLineCount).map { it.getStatValue() }.toList()

            val total = stats[0]
            logger.lifecycle("Artifact $artifactOrArchiveName, total methods: $total")

            if (project.kotlinBuildProperties.isTeamcityBuild) {
                println("##teamcity[buildStatisticValue key='DexMethodCount_${artifactOrArchiveName}' value='$total']")
            }

            ownPackages.map { packages ->
                val totalOwnPackages = stats[1]
                val totalOtherPackages = stats[2]

                logger.lifecycle("Artifact $artifactOrArchiveName, total methods from packages ${packages.joinToString { "$it.*" }}: $totalOwnPackages")
                logger.lifecycle("Artifact $artifactOrArchiveName, total methods from other packages: $totalOtherPackages")

                if (project.kotlinBuildProperties.isTeamcityBuild) {
                    println("##teamcity[buildStatisticValue key='DexMethodCount_${artifactOrArchiveName}_OwnPackages' value='$totalOwnPackages']")
                    println("##teamcity[buildStatisticValue key='DexMethodCount_${artifactOrArchiveName}_OtherPackages' value='$totalOtherPackages']")
                }
            }
        }
    }
}

fun Project.printStats(dexMethodCount: TaskProvider<DexMethodCount>) {
    val dexMethodCountStats = tasks.register("dexMethodCountStats", DexMethodCountStats::class.java) {
        dependsOn(dexMethodCount)
        inputFile.set(dexMethodCount.flatMap { objects.fileProperty().apply { set(it.detailOutputFile) } })
        artifactOrArchiveName.set(dexMethodCount.map { it.artifactOrArchiveName })
        ownPackages.set(dexMethodCount.flatMap { it.ownPackages })
    }

    dexMethodCount.configure {
        finalizedBy(dexMethodCountStats)
    }
}

fun Project.dexMethodCount(action: DexMethodCount.() -> Unit): TaskProvider<DexMethodCount> {
    val dexMethodCount = tasks.register("dexMethodCount", DexMethodCount::class.java, action)
    printStats(dexMethodCount)
    tasks.getByName("check").dependsOn(dexMethodCount)
    return dexMethodCount
}

private val DexMethod.`package`: String get() = declaringType.substringBeforeLast('.')
private fun Int.padRight() = toString().padStart(5, ' ')
