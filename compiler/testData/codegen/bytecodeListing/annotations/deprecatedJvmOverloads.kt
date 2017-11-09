// WITH_RUNTIME

class Foo {
    @JvmOverloads @Deprecated(message = "Foo") fun bar(x: String = "") {
    }
}
