// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class TopLevelValueClass(val s: String)

@get:JvmExposeBoxed
val lambda : () ->  TopLevelValueClass = {TopLevelValueClass("OK")}

// LIGHT_ELEMENTS_NO_DECLARATION: TopLevelValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]