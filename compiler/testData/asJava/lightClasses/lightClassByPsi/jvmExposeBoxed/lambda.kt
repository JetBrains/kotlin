// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class TopLevelValueClass(val s: String)

@get:JvmExposeBoxed
val lambda : () ->  TopLevelValueClass = {TopLevelValueClass("OK")}
