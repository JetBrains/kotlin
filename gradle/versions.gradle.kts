import org.gradle.api.artifacts.VersionCatalogsExtension

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun catalogVersion(tomlKey: String, extraKey: String) {
    extra["versions.$extraKey"] = catalog.findVersion(tomlKey).get().requiredVersion
}

catalogVersion("intellijSdk", "intellijSdk")
catalogVersion("intellij-annotations", "annotations")
catalogVersion("commons-lang", "commons-lang3")
catalogVersion("aalto-xml", "aalto-xml")
catalogVersion("gradle-api", "gradle-api")
catalogVersion("groovy-xml", "groovy-xml")
catalogVersion("groovy", "groovy")
catalogVersion("gson", "gson")
catalogVersion("jna-platform", "jna-platform")
catalogVersion("jna", "jna")
catalogVersion("log4j", "log4j")
catalogVersion("lz4-java", "lz4-java")
catalogVersion("oro", "oro")
catalogVersion("serviceMessages", "serviceMessages")
catalogVersion("stax2-api", "stax2-api")
catalogVersion("streamex", "streamex")
catalogVersion("lombok", "lombok")
catalogVersion("commons-compress", "commons-compress")
catalogVersion("commons-io", "commons-io")
catalogVersion("android-sdk", "android")
catalogVersion("ant", "ant")
catalogVersion("jansi", "jansi")
catalogVersion("vavr", "vavr")
catalogVersion("javax-inject", "javax.inject")
catalogVersion("jflex", "jflex")
catalogVersion("jline", "jline")
catalogVersion("jsr305", "jsr305")
catalogVersion("kotlin-reflect-bundled", "kotlin-reflect")
catalogVersion("kotlinx-collections-immutable-jvm", "kotlinx-collections-immutable-jvm")
catalogVersion("kotlinx-collections-immutable", "kotlinx-collections-immutable")
catalogVersion("kotlinx-metadata-klib", "kotlinx-metadata-klib")
catalogVersion("native-platform", "native-platform")
catalogVersion("protobufRelocated", "protobuf-relocated")
catalogVersion("r8", "r8")
catalogVersion("robolectric", "robolectric")
catalogVersion("nodejs", "nodejs")
catalogVersion("nodejs-lts", "nodejs.lts")
catalogVersion("nodejs-for-building-wasm-debug-browsers", "nodejs.for.building.wasm.debug.browsers")
catalogVersion("v8", "v8")
catalogVersion("binaryen", "binaryen")
catalogVersion("swc", "swc")
catalogVersion("jakarta-annotation-api", "jakarta.annotation-api")
catalogVersion("vertx-codegen", "vertx-codegen")
catalogVersion("kotlinx-coroutines-test-jvm", "kotlinx-coroutines-test-jvm")

// Computed properties

val gradleJars = listOf(
    "gradle-api",
    "gradle-tooling-api",
    "gradle-base-services",
    "gradle-wrapper",
    "gradle-core",
    "gradle-base-services-groovy"
)

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

extra["versions.platform"] = platform

for (jar in gradleJars) {
    extra["versions.jar.$jar"] = extra["versions.gradle-api"]
}

if (!extra.has("versions.androidStudioRelease")) {
    extra["ignore.jar.android-base-common"] = true
}
