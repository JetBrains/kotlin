// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT

open class Base() {
    companion {
        fun foo() = "ParentFoo"
        fun bar()  = "ParentBar"
        fun baz()  = "ParentBaz"
    }
}

class A : Base() {

    fun bazA()  = baz() + "Called"

    companion {
        fun foo() = "ChildFoo"
        fun barA() = bar() + "Called"
    }

}

fun box(): String {
    val res = A.foo() + A.barA() + A().bazA()
    return if(res == "ChildFooParentBarCalledParentBazCalled") "OK" else "FAIL: $res"
}
