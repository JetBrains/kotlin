import kotlin.reflect.KFunction

// --------------------------------------------------
// Exported items:
// --------------------------------------------------

// TODO: pack this as a Gradle plugin
val ultimateTools: Map<String, KFunction<Any>> = listOf<KFunction<Any>>(
    ::ijProductBranch,
    ::disableBuildTasks,

    ::addCidrDeps,
    ::addIdeaNativeModuleDeps,
    ::addKotlinGradleToolingDeps,
    ::handleSymlink,
    ::proprietaryRepositories
).map { it.name to it }.toMap()

rootProject.extensions.add("ultimateTools", ultimateTools)

// --------------------------------------------------
// Compatibility tasks:
// --------------------------------------------------

fun ijProductBranch(productVersion: String): Int {
    return productVersion.substringBefore(".", productVersion.substringBefore("-"))
        .toIntOrNull() ?: error("Invalid product version format: $productVersion")
}

// disable anything but "clean" and tasks from "help" group
// log the appropriate message
fun disableBuildTasks(project: Project, reason: String) = with(project) {
    val tasksToDisable = tasks.filter {
        it.enabled && it.name != "clean" && it.group != "help"
    }

    if (tasksToDisable.isNotEmpty()) {
        tasksToDisable.forEach { it.enabled = false }
        logger.warn("Build tasks in $project have been disabled due to condition mismatch: $reason: ${tasksToDisable.joinToString { it.name }}")
    }
}

// --------------------------------------------------
// Shared utils:
// --------------------------------------------------

val excludesListFromIdeaPlugin: List<String> by rootProject.extra
val isStandaloneBuild: Boolean by rootProject.extra
val cidrPluginsEnabled: Boolean? by rootProject.extra

val javaApiArtifacts = listOf("java-api", "java-impl")

data class IDE(val name: String, val version: String)

fun guessIDEParams(): IDE {
    if (rootProject.extra.has("versions.androidStudioBuild")) {
        return IDE("android-studio-ide", rootProject.extra["versions.androidStudioBuild"] as String)
    }

    val ideName = if ((rootProject.extra["intellijUltimateEnabled"] as? Boolean) == true)
        "ideaIU"
    else
        "ideaIC"

    val ideVersion = rootProject.extra["versions.intellijSdk"] as String
    return IDE(ideName, ideVersion)
}

fun addIdeaNativeModuleDepsComposite(project: Project) = with(project) {
    dependencies {
        // Gradle projects with Kotlin/Native-specific logic
        // (automatically brings all the necessary transient dependencies, include deps on IntelliJ platform)
        add("compile", project(":idea:idea-native"))
        add("compile", project(":idea:idea-gradle-native"))
        add("compile", project(":idea:kotlin-gradle-tooling"))
        add("compile", project(":native:kotlin-native-utils"))
        add("implementation", project(":idea:jvm-debugger:jvm-debugger-core")) { isTransitive = false }

        val (ideName, ideVersion) = guessIDEParams()

        val ideBranch = ijProductBranch(ideVersion)
        val javaModuleName = if (ideBranch >= 192) "java" else ideName

        add("compile", "kotlin.build:$javaModuleName:$ideVersion") {
            javaApiArtifacts.forEach { jarName ->
                artifact {
                    name = jarName
                    type = "jar"
                    extension = "jar"
                }
            }
            isTransitive = false
        }

        val ijPlatformDependencies = mutableListOf(
            "idea",
            "platform-api",
            "platform-impl",
            "util",
            "extensions",
            "jdom"
        )

        if (ideBranch >= 192) {
            ijPlatformDependencies += listOf(
                "intellij-dvcs",
                "platform-concurrency",
                "platform-core-ui",
                "platform-util-ui",
                "platform-util-ex"
            )
        }

        if (ideBranch <= 193) {
            ijPlatformDependencies += listOf(
                "openapi"
            )
        }

        if (ideBranch >= 193) {
            ijPlatformDependencies += listOf(
                "platform-ide-util-io"
            )
        }

        add("compile", "kotlin.build:$ideName:$ideVersion") {
            ijPlatformDependencies.forEach { jarName ->
                artifact {
                    name = jarName
                    type = "jar"
                    extension = "jar"
                }
            }
            isTransitive = false
        }

        add("compile", "kotlin.build:gradle:$ideVersion")
    }
}

fun addIdeaNativeModuleDepsStandalone(project: Project) = with(project) {
    dependencies {
        // contents of Kotlin plugin
        val ideaPluginForCidrDir: String by rootProject.extra
        val ideaPluginJars = fileTree(ideaPluginForCidrDir) {
            exclude(excludesListFromIdeaPlugin)
        }
        add("compile", ideaPluginJars)

        val version = rootProject.extra[if (isStandaloneBuild) "cidrVersion" else "versions.intellijSdk"] as String

        add("compile", "com.jetbrains.intellij.platform:debugger-impl:$version")
        add("compile", "com.jetbrains.intellij.platform:indexing-impl:$version")
        add("compile", "com.jetbrains.intellij.platform:ide-impl:$version")
        add("compile", "com.jetbrains.intellij.platform:lang-impl:$version")
        add("compile", "com.jetbrains.intellij.platform:external-system:$version")
        add("compile", "com.jetbrains.intellij.platform:external-system-impl:$version")
        add("compile", "com.jetbrains.intellij.gradle:gradle-common:$version")
        add("compile", "com.jetbrains.intellij.gradle:gradle-tooling-extension-impl:$version")
        add("compile", "com.jetbrains.intellij.java:java-debugger-impl:$version")
        add("compile", "com.jetbrains.intellij.java:java-psi-impl:$version")
        add("compile", "com.jetbrains.intellij.java:java-compiler-impl:$version")
        add("compile", "com.jetbrains.intellij.java:java-execution:$version")
    }
}

fun addIdeaNativeModuleDeps(project: Project) {
    val cidrVersion: String by rootProject.extra
    with(project) {
        proprietaryRepositories(project)
        dependencies {
            add("compile", "com.jetbrains.intellij.c:c:$cidrVersion") { isTransitive = isStandaloneBuild }
            add("compile", "com.jetbrains.intellij.cidr:cidr-common:$cidrVersion") { isTransitive = isStandaloneBuild }
            add("compile", "com.jetbrains.intellij.cidr:cidr-debugger:$cidrVersion") { isTransitive = isStandaloneBuild }
        }
    }
    if (isStandaloneBuild) addIdeaNativeModuleDepsStandalone(project) else addIdeaNativeModuleDepsComposite(project)
}

fun addCidrDeps(project: Project) = with(project) {
    val nativeDebugPluginDir: File? by rootProject.extra

    if (nativeDebugPluginDir?.exists() == true) { // Idea Ultimate build
        dependencies {
            add("compile", fileTree(nativeDebugPluginDir!!) { include("**/*.jar") })
        }
    }
}

fun addKotlinGradleToolingDepsComposite(project: Project) = with(project) {
    dependencies {
        add("compileOnly", project(":idea:kotlin-gradle-tooling"))
    }
}

fun addKotlinGradleToolingDepsStandalone(project: Project) = with(project) {
    dependencies {
        // Kotlin Gradle tooling & Kotlin stdlib
        val ideaPluginForCidrDir: String by rootProject.extra
        val kotlinGradleToolingJars = fileTree(ideaPluginForCidrDir) {
            include("lib/kotlin-gradle-tooling.jar")
            include("lib/kotlin-stdlib.jar")
        }
        add("compileOnly", kotlinGradleToolingJars)
    }
}

fun addKotlinGradleToolingDeps(project: Project) {
    if (isStandaloneBuild) addKotlinGradleToolingDepsStandalone(project) else addKotlinGradleToolingDepsComposite(project)

    with(project) {
        proprietaryRepositories(project)
        dependencies {
            val version = rootProject.extra[if (isStandaloneBuild) "cidrVersion" else "versions.intellijSdk"] as String
            add("compileOnly", "com.jetbrains.intellij.gradle:gradle-common:$version")
        }
    }
}

fun handleSymlink(details: FileCopyDetails, targetDir: File): Boolean = with(details) {
    val symlink = file.toPath().firstParentSymlink()
    if (symlink != null) {
        exclude()
        val destPath = relativeTo(targetDir, symlink)
        if (java.nio.file.Files.notExists(destPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            if (java.nio.file.Files.notExists(destPath.parent)) {
                project.mkdir(destPath.parent)
            }
            java.nio.file.Files.createSymbolicLink(destPath, java.nio.file.Files.readSymbolicLink(symlink))
        }
        return true
    }
    return false
}

fun java.nio.file.Path.firstParentSymlink(): java.nio.file.Path? {
    var cur: java.nio.file.Path? = this
    @Suppress("SENSELESS_COMPARISON")
    while (cur != null) {
        if (java.nio.file.Files.isSymbolicLink(cur)) break
        cur = cur.parent
    }
    return cur
}

fun FileCopyDetails.relativeTo(targetDir: File, symlink: java.nio.file.Path): java.nio.file.Path {
    var srcRoot = file.toPath()
    var cur = java.nio.file.Paths.get(relativePath.pathString)
    @Suppress("SENSELESS_COMPARISON")
    while (cur != null) {
        srcRoot = srcRoot.parent
        cur = cur.parent
    }
    return targetDir.toPath().resolve(srcRoot.relativize(symlink))
}

fun proprietaryRepositories(project: Project) = with(project) {
    repositories {
        maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
        maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/")
        maven("https://cache-redirector.jetbrains.com/download.jetbrains.com/teamcity-repository")
    }
}