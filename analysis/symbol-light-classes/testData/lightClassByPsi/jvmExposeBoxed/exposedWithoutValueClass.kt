// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

// An explicitly annotated declaration whose signature contains no value-class types does not need a separate boxed wrapper.
// The JVM backend keeps a single declaration with `@JvmExposeBoxed`. For functions and accessors, `@JvmName` takes
// precedence; otherwise the `jvmName` argument of `@JvmExposeBoxed` supplies the Java name. Constructors cannot be renamed,
// so only annotation preservation is observable for them.

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
