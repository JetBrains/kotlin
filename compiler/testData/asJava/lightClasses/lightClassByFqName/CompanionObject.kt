// C
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

class C {
    companion object {
        @[kotlin.jvm.JvmField] public val foo: String = { "A" }()
    }
}
