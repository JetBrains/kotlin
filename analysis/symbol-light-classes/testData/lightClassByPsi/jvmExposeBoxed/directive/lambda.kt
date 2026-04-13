// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class TopLevelValueClass(val s: String)

val lambda : () ->  TopLevelValueClass = {TopLevelValueClass("OK")}

// LIGHT_ELEMENTS_NO_DECLARATION: TopLevelValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]