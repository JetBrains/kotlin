// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// MODULE: lib
// FILE: lib.kt
enum class Foo {
    FOO() {
        override fun foo() = "foo"
       
        override var xxx: String
            get() =  "xxx"
            set(value: String) {
            }
    };

    abstract fun foo(): String
    abstract var xxx: String
}

// MODULE: main(lib)
// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(Foo.FOO.foo(), "foo")
    Foo.FOO.xxx = "zzzz"
    assertEquals(Foo.FOO.xxx, "xxx")
    assertEquals(Foo.FOO.toString(), "FOO")
    assertEquals(Foo.valueOf("FOO").toString(), "FOO")
    return "OK"
}

