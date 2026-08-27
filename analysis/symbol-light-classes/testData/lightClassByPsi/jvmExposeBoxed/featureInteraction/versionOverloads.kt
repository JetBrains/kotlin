// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

class Exposed {
    @Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
    @JvmExposeBoxed("fooExposed")
    @JvmOverloads
    fun foo(
        a: Float = 1f,
        @IntroducedAt("3") b: UInt = 2u,
        @IntroducedAt("2") c: Boolean = true,
    ): String = "$a/$b/$c"
}

class ExposedAndRenamed {
    @Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
    @JvmExposeBoxed("fooExposed")
    @JvmName("fooRenamed")
    @JvmOverloads
    fun foo(
        a: Float = 1f,
        @IntroducedAt("3") b: UInt = 2u,
        @IntroducedAt("2") c: Boolean = true,
    ): String = "$a/$b/$c"
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Exposed.class[foo]
// LIGHT_ELEMENTS_NO_DECLARATION: Exposed.class[foo-OsBMiQA;foo-Qn1smSk;fooExposed;fooExposed;fooExposed;fooExposed;fooExposed], ExposedAndRenamed.class[fooExposed;fooExposed;fooRenamed]
