// LANGUAGE: +MultiPlatformProjects, +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM_IR
// IGNORE_HMPP: JVM_IR
// Notes: Ignore JVM backend because JVM does not allow same signature on member and static

// MODULE: common
// FILE: common.kt

expect class A {
    val a: String
    fun foo(b: Int): String

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
    val res = A().a + A().foo(0) + A.a + A.foo(1) + A.Companion.a + A.Companion.foo(2)
    return if(res == "MemberMember0CompBlockCompBlock1CompObjCompObj2") "OK" else "NOT OK: $res"
}

// MODULE: platform()()(common)
// FILE: platform.kt


actual class A {

    actual val a = "Member"
    actual fun foo(b: Int): String = a + b.toString()

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
