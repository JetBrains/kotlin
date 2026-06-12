// LANGUAGE: +MultiPlatformProjects, +CompanionBlocksAndExtensions
// IGNORE_HMPP: JS_IR

// MODULE: common
// FILE: common.kt

expect class A {

    companion {
        val a: String
        fun foo(b: Int): String

    }
    companion object {
        val a: String
        fun foo(b: Int): String
    }
}

fun common(): String {
    val res = A.a + A.foo(1) + A.Companion.a + A.Companion.foo(2)
    return if(res == "BlockBlock1ObjObj2") "OK" else "NOT OK: $res"
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class A {

    companion {
        actual val a = "Block"
        actual fun foo(b: Int): String = a + b.toString()
    }

    actual companion object {
        actual val a = "Obj"
        actual fun foo(b: Int): String = a + b.toString()
    }
}

fun box(): String = common()
