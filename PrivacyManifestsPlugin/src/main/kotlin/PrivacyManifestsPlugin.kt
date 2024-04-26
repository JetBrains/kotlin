package org.kmp_apple_privacy_manifests

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

class PrivacyManifestsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            (target.kotlinExtension as ExtensionAware).extensions.create(
                PrivacyManifestConfiguration::class.java,
                "privacyManifestConfiguration",
                PrivacyManifestConfiguration::class.java,
            )
        }
    }

}

open class PrivacyManifestConfiguration @Inject constructor(
    private val project: Project,
) {

    private var configureOnce = false
    fun embedPrivacyManifest(
        privacyManifest: File,
        resourceBundleInfoPlist: File? = null,
        resourceBundleName: String = "KotlinMultiplatformPrivacyManifest",
    ) {
        if (configureOnce) error("Already configured")
        configureOnce = true

        project.pluginManager.withPlugin("org.jetbrains.kotlin.native.cocoapods") {
            ((project.kotlinExtension as ExtensionAware).extensions.getByName("cocoapods") as CocoapodsExtension)
                .extraSpecAttributes["resource_bundles"] = "{'${resourceBundleName}' => ['${privacyManifest.toRelativeString(project.layout.projectDirectory.asFile)}']}"
        }

        val executesInEmbedAndSignContext = project.objects.property(Boolean::class.java)
        val executesInSyncFrameworkContext = project.objects.property(Boolean::class.java)
        project.gradle.taskGraph.whenReady { graph ->
            executesInEmbedAndSignContext.set(
                graph.allTasks.any { task ->
                    task.name.startsWith("embedAndSign") && task.name.endsWith("AppleFrameworkForXcode")
                }
            )
            executesInSyncFrameworkContext.set(
                graph.allTasks.any { task ->
                    task.name == "syncFramework"
                }
            )
        }

        // FIXME: In case of CocoaPods wire up resources into resources bundles
        project.tasks.withType(KotlinNativeLink::class.java).configureEach { linkTask ->
            if (linkTask.binary is Framework) {
                // FIXME: Check output file invalidation
                val targetBuildDir = project.providers.environmentVariable("TARGET_BUILD_DIR").orElse("")
                val resourcesPath = project.providers.environmentVariable("UNLOCALIZED_RESOURCES_FOLDER_PATH").orElse("")
                val outputPath = targetBuildDir.flatMap { targetBuildDirPath ->
                    resourcesPath.map { resourcesDir ->
                        File(targetBuildDirPath)
                            .resolve(resourcesDir)
                            .resolve("${resourceBundleName}.bundle")
                    }
                }
                linkTask.inputs.file(privacyManifest)

                // FIXME: Skip if embedAndSigning in .appex

                // FIXME: Check this doesn't fail
                val isInXcodeBuild = System.getenv("TARGET_BUILD_DIR") != null
                val isInCocoaPodsTargetXcodeBuild = System.getenv("PODS_TARGET_SRCROOT") != null
                if (isInXcodeBuild && !isInCocoaPodsTargetXcodeBuild) {
                    linkTask.inputs.property("targetBuildDir", targetBuildDir)
                    linkTask.inputs.property("resourcesPath", resourcesPath)
                    linkTask.outputs.dir(outputPath)
                }

                linkTask.doLast {
                    if (executesInSyncFrameworkContext.get()) return@doLast
                    if (executesInEmbedAndSignContext.get()) {
                        // FIXME: ???
                        val resourcesBundle = outputPath.get()

                        // FIXME: This may execute multiple times? Does this invalidate up-to-dateness
                        privacyManifest.copyTo(
                            resourcesBundle.resolve(privacyManifestName),
                            overwrite = true,
                        )
                    } else {
                        val frameworkPath = linkTask.outputFile.get()
                        privacyManifest.copyTo(
                            frameworkPath.resolve(privacyManifestName),
                            overwrite = true,
                        )
                    }
                }
            }
        }

        project.tasks.withType(FatFrameworkTask::class.java).configureEach { fatFrameworkTask ->
            fatFrameworkTask.inputs.file(privacyManifest)
            fatFrameworkTask.doLast {
                if (executesInSyncFrameworkContext.get()) return@doLast
                if (executesInEmbedAndSignContext.get()) return@doLast
//                val isIosBundling = fatFrameworkTask.frameworks.first().target in iosGroup
//                if (isIosBundling) {
//
//                }
                privacyManifest.copyTo(
                    fatFrameworkTask.fatFramework.resolve(privacyManifestName),
                    overwrite = true,
                )
            }
        }
    }

    companion object {
        val privacyManifestName = "PrivacyInfo.xcprivacy"

        val iosGroup: Set<KonanTarget> = setOf(
            KonanTarget.IOS_X64,
            KonanTarget.IOS_ARM64,
            KonanTarget.IOS_SIMULATOR_ARM64,
            // FIXME: watchOS etc
        )

        val macOSGroup: Set<KonanTarget> = setOf(
            KonanTarget.MACOS_ARM64,
            KonanTarget.MACOS_X64,
        )

        val appleTargets: Set<KonanTarget> = iosGroup + macOSGroup
    }
}
