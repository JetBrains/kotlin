/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.ProjectTopics
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.util.PathUtilRt
import org.jetbrains.kotlin.idea.caches.project.getModuleInfosFromIdeaModel
import org.jetbrains.kotlin.idea.configuration.KotlinNativeLibraryNameUtil
import org.jetbrains.kotlin.idea.versions.UnsupportedAbiVersionNotificationPanelProvider
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME

/** TODO: merge [KotlinNativeABICompatibilityChecker] in the future with [UnsupportedAbiVersionNotificationPanelProvider], KT-34525 */
class KotlinNativeABICompatibilityChecker(private val project: Project) : ProjectComponent, Disposable {

    private sealed class LibraryGroup(private val ordinal: Int) : Comparable<LibraryGroup> {

        override fun compareTo(other: LibraryGroup) = when {
            this == other -> 0
            this is FromDistribution && other is FromDistribution -> kotlinVersion.compareTo(other.kotlinVersion)
            else -> ordinal.compareTo(other.ordinal)
        }

        data class FromDistribution(val kotlinVersion: String) : LibraryGroup(0)
        object ThirdParty : LibraryGroup(1)
        object User : LibraryGroup(2)
    }

    private val cachedIncompatibleLibraries = HashSet<String>()

    init {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                // run when project roots are changes, e.g. on project import
                validateKotlinNativeLibraries()
            }
        })
    }

    override fun projectOpened() {
        // run when project is opened
        validateKotlinNativeLibraries()
    }

    private fun validateKotlinNativeLibraries() {
        if (ApplicationManager.getApplication().isUnitTestMode || project.isDisposed)
            return

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            object : Task.Backgroundable(project, BG_TASK_NAME) {
                override fun run(indicator: ProgressIndicator) {
                    val librariesToNotify = runReadAction {
                        if (project.isDisposed) return@runReadAction emptyMap<String, NativeLibraryInfo>()

                        getLibrariesToNotifyAbout()
                    }
                    if (project.isDisposed) return
                    val notifications = prepareNotifications(librariesToNotify)

                    notifications.forEach {
                        runInEdt {
                            it.notify(project)
                        }
                    }
                }
            },
            EmptyProgressIndicator()
        )
    }

    private fun getLibrariesToNotifyAbout(): Map<String, NativeLibraryInfo> = synchronized(this) {
        val incompatibleLibraries = getModuleInfosFromIdeaModel(project).asSequence()
            .filterIsInstance<NativeLibraryInfo>()
            .filter { !it.metadataInfo.isCompatible }
            .associateBy { it.libraryRoot }

        val newEntries = if (cachedIncompatibleLibraries.isNotEmpty())
            incompatibleLibraries.filterKeys { it !in cachedIncompatibleLibraries }
        else
            incompatibleLibraries

        cachedIncompatibleLibraries.clear()
        cachedIncompatibleLibraries.addAll(incompatibleLibraries.keys)

        return newEntries
    }

    private fun prepareNotifications(librariesToNotify: Map<String, NativeLibraryInfo>): List<Notification> {
        if (librariesToNotify.isEmpty())
            return emptyList()

        val librariesByGroups = HashMap<Pair<LibraryGroup, Boolean>, MutableList<Pair<String, String>>>()
        librariesToNotify.forEach { (libraryRoot, libraryInfo) ->
            val isOldMetadata = (libraryInfo.metadataInfo as? NativeLibraryInfo.MetadataInfo.Incompatible)?.isOlder ?: true
            val (libraryName, libraryGroup) = parseIDELibraryName(libraryInfo)
            librariesByGroups.computeIfAbsent(libraryGroup to isOldMetadata) { mutableListOf() } += libraryName to libraryRoot
        }

        return librariesByGroups.keys.sortedWith(
            compareBy(
                { (libraryGroup, _) -> libraryGroup },
                { (_, isOldMetadata) -> isOldMetadata }
            )
        ).map { key ->

            val (libraryGroup, isOldMetadata) = key
            val libraries =
                librariesByGroups.getValue(key).sortedWith(compareBy(LIBRARY_NAME_COMPARATOR) { (libraryName, _) -> libraryName })

            val compilerVersionText = if (isOldMetadata) "an older" else "a newer"

            val message = when (libraryGroup) {
                is LibraryGroup.FromDistribution -> {
                    val libraryNamesInOneLine =
                        libraries.joinToString(limit = MAX_LIBRARY_NAMES_IN_ONE_LINE) { (libraryName, _) -> libraryName }

                    """
                    |There are ${libraries.size} libraries from the Kotlin/Native ${libraryGroup.kotlinVersion} distribution attached to the project: $libraryNamesInOneLine
                    |
                    |These libraries were compiled with $compilerVersionText Kotlin/Native compiler and can't be read in IDE. Please edit Gradle buildfile(s) to use Kotlin Gradle plugin version ${bundledRuntimeVersion()}. Then re-import the project in IDE.
                    """.trimMargin()
                }
                is LibraryGroup.ThirdParty -> {
                    if (libraries.size == 1) {
                        """
                        |There is a third-party library attached to the project that was compiled with $compilerVersionText Kotlin/Native compiler and can't be read in IDE: ${libraries.single()
                            .first}
                        |
                        |Please edit Gradle buildfile(s) and specify library version compatible with Kotlin/Native ${bundledRuntimeVersion()}. Then re-import the project in IDE.
                        """.trimMargin()
                    } else {
                        val librariesLineByLine = libraries.joinToString(separator = "\n") { (libraryName, _) -> libraryName }

                        """
                        |There are ${libraries.size} third-party libraries attached to the project that were compiled with $compilerVersionText Kotlin/Native compiler and can't be read in IDE:
                        |$librariesLineByLine
                        |
                        |Please edit Gradle buildfile(s) and specify library versions compatible with Kotlin/Native ${bundledRuntimeVersion()}. Then re-import the project in IDE.
                        """.trimMargin()
                    }
                }
                is LibraryGroup.User -> {
                    val projectRoot = project.guessProjectDir()?.canonicalPath

                    fun getLibraryTextToPrint(libraryNameAndRoot: Pair<String, String>): String {
                        val (libraryName, libraryRoot) = libraryNameAndRoot

                        val relativeRoot = projectRoot?.let {
                            libraryRoot.substringAfter(projectRoot)
                                .takeIf { it != libraryRoot }
                                ?.trimStart('/', '\\')
                                ?.let { "${'$'}project/$it" }
                        } ?: libraryRoot

                        return "\"$libraryName\" at $relativeRoot"
                    }

                    if (libraries.size == 1) {
                        """
                        |There is a library attached to the project that was compiled with $compilerVersionText Kotlin/Native compiler and can't be read in IDE:
                        |${getLibraryTextToPrint(libraries.single())}
                        |
                        |Please edit Gradle buildfile(s) to use Kotlin Gradle plugin version ${bundledRuntimeVersion()}. Then rebuild the project and re-import it in IDE.
                        """.trimMargin()
                    } else {
                        val librariesLineByLine = libraries.joinToString(separator = "\n", transform = ::getLibraryTextToPrint)

                        """
                        |There are ${libraries.size} libraries attached to the project that were compiled with $compilerVersionText Kotlin/Native compiler and can't be read in IDE:
                        |$librariesLineByLine
                        |
                        |Please edit Gradle buildfile(s) to use Kotlin Gradle plugin version ${bundledRuntimeVersion()}. Then rebuild the project and re-import it in IDE.
                        """.trimMargin()
                    }
                }
            }

            Notification(
                NOTIFICATION_GROUP_ID,
                NOTIFICATION_TITLE,
                StringUtilRt.convertLineSeparators(message, "<br/>"),
                NotificationType.ERROR,
                null
            )
        }
    }

    // returns pair of library name and library group
    private fun parseIDELibraryName(libraryInfo: NativeLibraryInfo): Pair<String, LibraryGroup> {
        val ideLibraryName = libraryInfo.library.name?.takeIf(String::isNotEmpty)
        if (ideLibraryName != null) {
            KotlinNativeLibraryNameUtil.parseIDELibraryName(ideLibraryName)?.let { (kotlinVersion, libraryName) ->
                return libraryName to LibraryGroup.FromDistribution(kotlinVersion)
            }

            if (KotlinNativeLibraryNameUtil.isGradleLibraryName(ideLibraryName))
                return ideLibraryName to LibraryGroup.ThirdParty
        }

        return (ideLibraryName ?: PathUtilRt.getFileName(libraryInfo.libraryRoot)) to LibraryGroup.User
    }

    override fun dispose() = synchronized(this) {
        cachedIncompatibleLibraries.clear()
    }

    companion object {
        private val LIBRARY_NAME_COMPARATOR = Comparator<String> { libraryName1, libraryName2 ->
            when {
                libraryName1 == libraryName2 -> 0
                libraryName1 == KONAN_STDLIB_NAME -> -1 // stdlib must go the first
                libraryName2 == KONAN_STDLIB_NAME -> 1
                else -> libraryName1.compareTo(libraryName2)
            }
        }

        private const val MAX_LIBRARY_NAMES_IN_ONE_LINE = 5

        private const val NOTIFICATION_TITLE = "Incompatible Kotlin/Native libraries"
        private const val NOTIFICATION_GROUP_ID = NOTIFICATION_TITLE

        private const val BG_TASK_NAME = "Finding incompatible Kotlin/Native libraries"
    }
}
