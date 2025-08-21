// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper(val i: Int)

class Foo {
    var foo: IntWrapper get() = IntWrapper(0)
        set(value) {

        }
}
