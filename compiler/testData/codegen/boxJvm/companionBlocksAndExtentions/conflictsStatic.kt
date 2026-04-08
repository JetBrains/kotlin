// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// LANGUAGE: +CompanionBlocksAndExtensions

class A {

    companion object {
        @JvmStatic
        val a: String = "companionObjectVal"
        @JvmStatic
        fun foo(s: String) = "companionObjectFun: $s"
    }
    companion {
        val a: String = "companionBlockVal"
        fun foo(s: String) = "companionBlockFun: $s"
    }

}


fun box(): String {
    return "OK"
}
