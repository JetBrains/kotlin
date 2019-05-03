import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.DynamicObjectAware
import java.io.File
import java.util.*

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface PropertiesProvider {
    val rootProjectDir: File
    fun getProperty(key: String): Any?
}

class KotlinBuildProperties(
    private val propertiesProvider: PropertiesProvider
) {
    private val localProperties: Properties = Properties()

    init {
        val localPropertiesFile = propertiesProvider.rootProjectDir.resolve("local.properties")
        if (localPropertiesFile.isFile) {
            localPropertiesFile.reader().use(localProperties::load)
        }
    }

    private operator fun get(key: String): Any? = localProperties.getProperty(key) ?: propertiesProvider.getProperty(key)

    private fun getBoolean(key: String): Boolean = this[key]?.toString() == "true"

    val isJpsBuildEnabled: Boolean = getBoolean("jpsBuild")

    val isInIdeaSync: Boolean = run {
        // "idea.sync.active" was introduced in 2019.1
        System.getProperty("idea.sync.active")?.toBoolean() == true || let {
            // before 2019.1 there is "idea.active" that was true only on sync,
            // but since 2019.1 "idea.active" present in task execution too.
            // So let's check Idea version
            val majorIdeaVersion = System.getProperty("idea.version")
                ?.split(".")
                ?.getOrNull(0)
            val isBeforeIdea2019 = majorIdeaVersion == null || majorIdeaVersion.toInt() < 2019

            isBeforeIdea2019 && System.getProperty("idea.active")?.toBoolean() == true
        }
    }

    val isInJpsBuildIdeaSync: Boolean
        get() = isJpsBuildEnabled && isInIdeaSync

    val includeJava9: Boolean
        get() = !isInJpsBuildIdeaSync

    val useBootstrapStdlib: Boolean
        get() = isInJpsBuildIdeaSync
}

private const val extensionName = "kotlinBuildFlags"

class ProjectProperties(val project: Project): PropertiesProvider {
    override val rootProjectDir: File
        get() = project.projectDir

    override fun getProperty(key: String): Any? = project.findProperty(key)
}

val Project.kotlinBuildProperties: KotlinBuildProperties
    get() = rootProject.extensions.findByName(extensionName) as KotlinBuildProperties?
        ?: KotlinBuildProperties(ProjectProperties(rootProject)).also {
            rootProject.extensions.add(extensionName, it)
        }

class SettingsProperties(val settings: Settings): PropertiesProvider {
    override val rootProjectDir: File
        get() = settings.rootDir

    override fun getProperty(key: String): Any? {
        val obj = (settings as DynamicObjectAware).asDynamicObject
        return if (obj.hasProperty(key)) obj.getProperty(key) else null
    }
}

fun getKotlinBuildPropertiesForSettings(settings: Any) = (settings as Settings).kotlinBuildProperties

val Settings.kotlinBuildProperties: KotlinBuildProperties
    get() = extensions.findByName(extensionName) as KotlinBuildProperties?
        ?: KotlinBuildProperties(SettingsProperties(this)).also {
            extensions.add(extensionName, it)
        }