// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

class Foo {
    @JvmOverloads @Deprecated(message = "Foo") fun bar(x: String = "") {
    }
}
