/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.jakewharton.dex.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import java.io.File

open class DexMethodCount : DefaultTask() {

    data class Counts(
            val total: Int,
            val totalOwnPackages: Int?,
            val totalOtherPackages: Int?,
            val byPackage: Map<String, Int>,
            val byClass: Map<String, Int>
    )

    @InputFile
    lateinit var jarFile: File

    @Input
    @Optional
    var ownPackages: List<String>? = null

    // ##teamcity[buildStatisticValue key='DexMethodCount_${artifactOrArchiveName}' value='62362']
    @Input
    @Optional
    var teamCityStatistics: Boolean? = null

    @Input
    @Optional
    var artifactName: String? = null

    private val artifactOrArchiveName get() = artifactName ?: project.name

    fun from(jar: Jar) {
        jarFile = jar.archivePath
        artifactName = jar.baseName
        dependsOn(jar)
    }

    lateinit var counts: Counts

    @TaskAction
    fun invoke() {
        val methods = DexMethods.list(jarFile)

        val counts = methods.getCounts().also { this.counts = it }

        printTotals(counts)
        printTCStats(counts)
        outputDetails(counts)
    }

    private fun List<DexMethod>.getCounts(): Counts {
        val byPackage = this.groupingBy { it.`package` }.eachCount()
        val byClass = this.groupingBy { it.declaringType }.eachCount()

        val ownPackages = ownPackages?.map { it + '.' }
        val byOwnPackages = if (ownPackages != null) {
            this.partition { method -> ownPackages.any { method.declaringType.startsWith(it) }}.let {
                it.first.size to it.second.size
            }
        } else (null to null)

        return Counts(total = this.size,
                      totalOwnPackages = byOwnPackages.first,
                      totalOtherPackages = byOwnPackages.second,
                      byPackage = byPackage,
                      byClass = byClass)
    }

    private fun printTotals(counts: Counts) {
        logger.lifecycle("Artifact $artifactOrArchiveName, total methods: ${counts.total}")
        ownPackages?.let { packages ->
            logger.lifecycle("Artifact $artifactOrArchiveName, total methods from packages ${packages.joinToString { "$it.*" }}: ${counts.totalOwnPackages}")
            logger.lifecycle("Artifact $artifactOrArchiveName, total methods from other packages: ${counts.totalOtherPackages}")
        }
    }

    private fun printTCStats(counts: Counts) {
        if (teamCityStatistics ?: project.hasProperty("teamcity")) {
            println("##teamcity[buildStatisticValue key='DexMethodCount_${artifactOrArchiveName}' value='${counts.total}']")
            counts.totalOwnPackages?.let { value ->
                println("##teamcity[buildStatisticValue key='DexMethodCount_${artifactOrArchiveName}_OwnPackages' value='$value']")
            }
            counts.totalOtherPackages?.let { value ->
                println("##teamcity[buildStatisticValue key='DexMethodCount_${artifactOrArchiveName}_OtherPackages' value='$value']")
            }
        }
    }

    private fun outputDetails(counts: Counts) {
        val detailFile = project.buildDir.resolve("$artifactOrArchiveName-method-count.txt")
        detailFile.printWriter().use { writer ->
            writer.println("${counts.total.padRight()}\tTotal methods")
            ownPackages?.let { packages ->
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


private val DexMethod.`package`: String get() = declaringType.substringBeforeLast('.')
private fun Int.padRight() = toString().padStart(5, ' ')
