/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.sampler

data class SampleInfo(
    val id: Int,
    val name: String,
    val description: String,
    val libraries: List<String>,
    val tags: List<String>
) {
    override fun toString(): String {
        return name
    }
}

class Sample(
    val id: Int,
    val name: String,
    val description: String,
    val libraries: List<String>,
    val tags: List<String>,
    val files: List<File>,
    val type: BuildSystemType
)

enum class BuildSystemType {
    MAVEN, GRADLE, NONE
}

data class File(
    val id: Int,
    val snapshotId: Int,
    val path: String,
    val name: String,
    val extension: String,
    val content: String
) {
    fun fullPath() : String = "$path/$name.$extension"
}

object SamplerInteraction {
    private val samples = (1..10).map { nameNo ->
        SampleInfo(
            nameNo,
            "Name$nameNo",
            "Description$nameNo",
            (1..nameNo).map { "Library$it" },
            (nameNo..10).map { "Tag$it" }
        )
    }

    fun getSamplerInfos(
        namePattern: String = "",
        libraries: List<String> = emptyList(),
        tags: List<String> = emptyList()
    ): List<SampleInfo> {
        return samples.filter { it.name.contains(namePattern) }
            .filter { it.libraries.containsAll(libraries) }
            .filter { it.tags.containsAll(tags) }
    }

    fun getSample(id: Int): Sample? {
        return samples.get(id).let {
            Sample(
                it.id,
                it.name,
                it.description,
                it.libraries,
                it.tags,
                listOf(
                    File(
                        1,
                        100 + id,
                        "",
                        "build",
                        "gradle",
                        buldGradle
                    ),
                    File(
                        id, 100 + id, "src/main/kotlin", "app", "kt", "fun main(){\n" +
                                "    println(\"Hello world!\")\n" +
                                "}"
                    )
                ),
                BuildSystemType.GRADLE
            )
        }
    }

    private val buldGradle = """
        plugins {
            id 'java'
            id 'org.jetbrains.kotlin.jvm' version '1.3.40'
        }
        
        group 'asd'
        version '1.0-SNAPSHOT'
        
        sourceCompatibility = 1.8
        
        repositories {
            maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
            mavenCentral()
        }
        
        dependencies {
            implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
            testCompile group: 'junit', name: 'junit', version: '4.12'
        }
        
        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
    """.trimIndent()
}