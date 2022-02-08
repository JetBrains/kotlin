// WITH_STDLIB

class Foo {
    @JvmOverloads @Deprecated(message = "Foo") fun bar(x: String = "") {
    }
}
