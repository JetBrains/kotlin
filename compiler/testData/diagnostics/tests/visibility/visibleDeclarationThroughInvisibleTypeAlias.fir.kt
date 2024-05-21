// FILE: Foo.kt
private typealias Attribute = PlatformAttr
internal class PlatformAttr

// FILE: Main.kt
fun main() {
    <!INVISIBLE_REFERENCE("constructor(): PlatformAttr; public; '/PlatformAttr'")!>Attribute<!>()
    PlatformAttr()
}