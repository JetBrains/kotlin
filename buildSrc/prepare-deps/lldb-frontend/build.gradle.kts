import org.w3c.dom.NamedNodeMap
import java.io.File
import java.io.FileNotFoundException
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    base
}

rootProject.apply {
    from(project.file("../../../gradle/kotlinUltimateProperties.gradle.kts"))
}

val server = "https://buildserver.labs.intellij.net"
val login = "guest"
val password = "guest"

// this module is being built for each gradle invocation
// while download should happen only for plugin construction
if (rootProject.extra.has("lldbFrontendHash") && rootProject.hasProperty("pluginVersion")) {
    val lldbFrontendLinuxRepo: String by rootProject.extra
    val lldbFrontendLinuxDir: File by rootProject.extra
    val lldbFrontendLinuxArtifact: String by rootProject.extra

    val lldbFrontendMacosRepo: String by rootProject.extra
    val lldbFrontendMacosDir: File by rootProject.extra
    val lldbFrontendMacosArtifact: String by rootProject.extra

    val lldbFrontendWindowsRepo: String by rootProject.extra
    val lldbFrontendWindowsDir: File by rootProject.extra
    val lldbFrontendWindowsArtifact: String by rootProject.extra

    val downloadLldbFrontendLinux: Task by downloading(lldbFrontendLinuxRepo, lldbFrontendLinuxDir, lldbFrontendLinuxArtifact)
    val downloadLldbFrontendMacos: Task by downloading(lldbFrontendMacosRepo, lldbFrontendMacosDir, lldbFrontendMacosArtifact)
    val downloadLldbFrontendWindows: Task by downloading(lldbFrontendWindowsRepo, lldbFrontendWindowsDir, lldbFrontendWindowsArtifact)

    tasks["build"].dependsOn(
        downloadLldbFrontendLinux,
        downloadLldbFrontendMacos,
        downloadLldbFrontendWindows
    )
}

fun NamedNodeMap.attribute(name: String): String? {
    return this.getNamedItem(name)?.toString()?.removePrefix("$name=\"")?.removeSuffix("\"")
}

fun Project.downloading(
    buildType: String,
    targetDir: File,
    artifactPath: String
) = tasks.creating {
    val isUpToDate = targetDir.isDirectory && targetDir.walkTopDown().firstOrNull { !it.isDirectory } != null
    outputs.upToDateWhen { isUpToDate }
    val lldbFrontendHash: String by rootProject.extra

    if (!isUpToDate) {
        doFirst {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(login, password.toCharArray())
            })

            val buildsUrl = "$server/httpAuth/app/rest/builds?locator=state:finished,branch:default:any,buildType:$buildType,revision:$lldbFrontendHash"
            val buildNumbers = ArrayList<String>()

            val result = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(buildsUrl)
            val nodeList = result.getElementsByTagName("build")

            for (id in 0 until nodeList.length) {
                val node = nodeList?.item(id)?.attributes?.let {
                    if (it.attribute("status") == "SUCCESS" && it.attribute("pinned") == "true") {
                        it.attribute("number")?.let { newNumber -> buildNumbers.add(newNumber) }
                    }
                }
            }

            for (buildNumber in buildNumbers) {
                try { // TC may accidentally delete artifacts of pinned builds
                    val artifactUrl = URL("$server/httpAuth/repository/download/$buildType/$buildNumber/$artifactPath")

                    targetDir.mkdirs()
                    File(targetDir, File(artifactUrl.path).name).let {
                        it.writeBytes(artifactUrl.readBytes())
                        it.setExecutable(true)
                    }

                    return@doFirst

                } catch (e: FileNotFoundException) { }
            }

            error("Unable to find build for commit $lldbFrontendHash in $buildType")
        }
    }
}
