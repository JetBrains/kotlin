import java.io.InputStream
import java.util.jar.Manifest
import java.util.zip.ZipFile

val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null

val distDir: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

val checkMavenArtifacts = tasks.register("checkMavenArtifacts") {
    val repoDir = rootProject.layout.buildDirectory.dir("repo")
    doLast {
        fileTree(repoDir).checkArtifacts { zip ->
            if (!zip.name.endsWith("-sources.jar"))
                zip.checkCompilerVersion(kotlinVersion)

            zip.checkManifest(kotlinVersion)
        }
    }
}

val checkDist = tasks.register("checkDistArtifacts") {
    doLast {
        fileTree(distDir).checkArtifacts { zip ->
            zip.checkCompilerVersion(kotlinVersion)
            zip.checkPluginXmlVersion(kotlinVersion)
        }
    }
}

tasks.register("checkArtifacts") {
    dependsOn(checkDist)
    dependsOn(checkMavenArtifacts)
}

fun FileTree.checkArtifacts(action: (zip: ZipFile) -> Unit) {
    filter { it.extension == "jar" }.forEach { jar ->
        val zip = ZipFile(jar)

        if (isTeamcityBuild)
            testStarted(zip.testName())

        action(zip)

        if (isTeamcityBuild)
            testFinished(zip.testName())
    }
}

fun ZipFile.checkManifest(version: String) = checkZipEntry("META-INF/MANIFEST.MF") { entryStream ->
    val implementationVersion = Manifest(entryStream).mainAttributes.getValue("Implementation-Version")
    "Manifest contains invalid 'Implementation-Version' value, expected: $version found: $implementationVersion".takeIf {
        implementationVersion?.let { it != version } ?: false
    }
}

fun ZipFile.checkCompilerVersion(version: String) = checkZipEntry("META-INF/compiler.version") {
    val artifactVersion = it.bufferedReader().readLine()
    "Invalid compiler.version content, expected: $version found: $artifactVersion"
        .takeIf { artifactVersion != version }
}

fun ZipFile.checkPluginXmlVersion(version: String) = checkZipEntry("META-INF/plugin.xml") { inputStream ->
    val pluginVersion = inputStream.bufferedReader()
        .lineSequence()
        .mapNotNull { Regex("""<version>([^<]+)</version>""").find(it) }
        .firstOrNull()
        ?.groupValues
        ?.get(1) ?: return@checkZipEntry "Plugin version not found in plugin.xml"

    "Invalid plugin version, expected version starting with '$version', actual: '$pluginVersion'"
        .takeIf { !pluginVersion.startsWith(version) }
}

fun ZipFile.checkZipEntry(entryName: String, action: (entryStream: InputStream) -> String?) {
    getEntry(entryName)
        ?.let { entry -> getInputStream(entry).use(action) }
        ?.let { reportProblem(testName(), it) }
}

fun ZipFile.testName() = file(name).relativeTo(file(rootDir)).invariantSeparatorsPath

fun reportProblem(artifact: String, message: String) {
    if (isTeamcityBuild)
        testFailed(artifact, "Artifact contains problems", message)
    else
        println("Artifact $artifact contains problems:\n$message")

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