// LANGUAGE: +MultiPlatformProjects, +CompanionBlocksAndExtensions


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
    return if(res == "CompBlockCompBlock1CompObjCompObj2") "OK" else "NOT OK: $res"
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class A {

    companion {
        actual val a = "CompBlock"
        actual fun foo(b: Int): String = a + b.toString()
    }

    actual companion object {
        actual val a = "CompObj"
        actual fun foo(b: Int): String = a + b.toString()
    }
}

fun box(): String = common()
