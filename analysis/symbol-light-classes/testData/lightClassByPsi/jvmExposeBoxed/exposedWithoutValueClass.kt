// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

// An explicit 'JvmExposeBoxed' on a signature with no value class has nothing to box, so the JVM backend doesn't split the
// declaration in two. The single declaration keeps the annotation, and its name comes from 'JvmExposeBoxed' unless 'JvmName'
// overrides it. A constructor cannot be renamed, so only the annotation is observable there.

class Exposed @JvmExposeBoxed constructor(val s: String) {
    @JvmExposeBoxed("renamed")
    fun withExposedName(s: String): String = s

    @JvmExposeBoxed
    fun withDefaultName(s: String): String = s

    @JvmExposeBoxed("exposedName")
    @JvmName("jvmName")
    fun withBothNames(s: String): String = s

    @get:JvmExposeBoxed("getRenamed")
    @set:JvmExposeBoxed("setRenamed")
    var renamedProperty: String = ""
}

@JvmExposeBoxed("topLevelRenamed")
fun topLevel(s: String): String = s

// DECLARATIONS_NO_LIGHT_ELEMENTS: Exposed.class[withExposedName], ExposedWithoutValueClassKt.class[topLevel]
// LIGHT_ELEMENTS_NO_DECLARATION: Exposed.class[getRenamed;renamed;setRenamed], ExposedWithoutValueClassKt.class[topLevelRenamed]
