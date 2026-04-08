// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// LANGUAGE: +CompanionBlocksAndExtensions

class A {
    val a: String = "memberVal"
    fun foo(s: String) = "memberFun: $s"

    companion {
        val a: String = "companionBlockVal"
        fun foo(s: String) = "companionBlockFun: $s"
    }

}

fun box(): String {
    return "OK"
}
