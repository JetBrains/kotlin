import java.util.*
import java.io.*

val scriptDirectory: File = File(buildscript.sourceURI!!.rawPath).parentFile
val propertiesFile: File = File(scriptDirectory , "versions.properties")

FileReader(propertiesFile).use {
    val properties = Properties()
    properties.load(it)
    properties.forEach { (k, v) ->
        extra[k.toString()] = v
    }
}

val androidStudioVersion = if (extra.has("versions.androidStudioRelease"))
    extra["versions.androidStudioRelease"]?.toString()?.replace(".", "")?.substring(0, 2)
else
    null

val intellijVersion = extra["versions.intellijSdk"] as String
val intellijVersionDelimiterIndex = intellijVersion.indexOfAny(charArrayOf('.', '-'))
if (intellijVersionDelimiterIndex == -1) {
    error("Invalid IDEA version $intellijVersion")
}

val platformBaseVersion = intellijVersion.substring(0, intellijVersionDelimiterIndex)
val platform = androidStudioVersion?.let { "AS$it" } ?: platformBaseVersion

rootProject.extra["versions.platform"] = platform

if (!extra.has("versions.androidStudioRelease")) {
    extra["ignore.jar.android-base-common"] = true
}
