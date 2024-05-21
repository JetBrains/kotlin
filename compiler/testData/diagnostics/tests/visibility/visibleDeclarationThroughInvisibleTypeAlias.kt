// FILE: Foo.kt
private typealias Attribute = PlatformAttr
internal class PlatformAttr

// FILE: Main.kt
fun main() {
    <!INVISIBLE_MEMBER("Attribute; private; file")!>Attribute<!>()
    PlatformAttr()
}