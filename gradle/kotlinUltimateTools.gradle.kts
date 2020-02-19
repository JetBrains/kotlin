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
    ::handleSymlink
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
val platformDepsJarName: String by rootProject.extra
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

        // IntelliJ platform (out of CIDR IDE distribution)
        val cidrIdeDir: String by rootProject.extra
        val cidrPlatform = fileTree(cidrIdeDir) {
            include("lib/*.jar")
            exclude("lib/kotlin*.jar") // because Kotlin should be taken from Kotlin plugin
            exclude("lib/clion*.jar") // don't take scrambled JARs
            exclude("lib/appcode*.jar")
        }
        add("compile", cidrPlatform)

        // standard CIDR plugins
        val cidrPlugins = fileTree(cidrIdeDir) {
            include("plugins/cidr-*/lib/*.jar")
            include("plugins/clion-*/lib/*.jar")
            include("plugins/appcode-*/lib/*.jar")
            include("plugins/gradle/lib/*.jar")
        }
        add("compile", cidrPlugins)

        // Java APIs (private artifact that goes together with CIDR IDEs)
        val cidrPlatformDepsOrJavaPluginDir: String by rootProject.extra
        val cidrPlatformDepsOrJavaPlugin = fileTree(cidrPlatformDepsOrJavaPluginDir) {
            include(platformDepsJarName)
            javaApiArtifacts.forEach { include("$it*.jar") }
        }
        add("compile", cidrPlatformDepsOrJavaPlugin)
    }
}

fun addIdeaNativeModuleDeps(project: Project) =
    if (isStandaloneBuild) addIdeaNativeModuleDepsStandalone(project) else addIdeaNativeModuleDepsComposite(project)

fun addCidrDeps(project: Project) = with(project) {
    val cidrUnscrambledJarDir: File? by rootProject.extra
    val nativeDebugPluginDir: File? by rootProject.extra

    if (nativeDebugPluginDir?.exists() == true) { // Idea Ultimate build
        dependencies {
            add("compile", fileTree(nativeDebugPluginDir!!) { include("**/*.jar") })
        }
    } else if (cidrUnscrambledJarDir?.exists() == true) { // CIDR build
        dependencies {
            add("compile", fileTree(cidrUnscrambledJarDir!!) { include("**/*.jar") })
        }
    }
}

fun addKotlinGradleToolingDepsComposite(project: Project) = with(project) {
    dependencies {
        add("compileOnly", project(":idea:kotlin-gradle-tooling"))

        val ideVersion = rootProject.extra["versions.intellijSdk"] as String
        add("compileOnly", "kotlin.build:gradle:$ideVersion")
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

        // Gradle plugin & Gradle API
        val cidrIdeDir: String by rootProject.extra
        val gradlePlugin = fileTree(cidrIdeDir) {
            include("lib/gradle-api*.jar")
            include("plugins/gradle/lib/*.jar")
        }
        add("compileOnly", gradlePlugin)
    }
}

fun addKotlinGradleToolingDeps(project: Project) =
    if (isStandaloneBuild) addKotlinGradleToolingDepsStandalone(project) else addKotlinGradleToolingDepsComposite(project)

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
