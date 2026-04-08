// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND_K1: ANY

open class Base() {
    companion {
        fun foo() = "parentFoo"
        fun bar()  = "parentBar"
        fun baz()  = "parentBaz"
    }
}

companion fun Base.xyz() = "parentXyz"

class A : Base() {
    companion {
        fun foo() = "aFoo"
        fun xyzb() = xyz()
    }
    fun bar() = "aBar"
    fun bazB()  = baz()
}

fun box(): String { //todo: assertEquals instead of if &&
    return if(Base.baz() == A().bazB() && A.foo() == "aFoo" && A().bar() == "aBar" && A.xyzb() == "parentXyz" ) "OK" else "FAIL"
}
