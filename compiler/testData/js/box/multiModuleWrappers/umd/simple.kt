// MODULE: module1
// JS_MODULE_KIND: UMD
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module1)
// JS_MODULE_KIND: UMD
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
