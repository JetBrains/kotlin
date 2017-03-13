//inspired by kt3492
interface BK {
    fun foo(): String
    fun bar(): String
}

interface KTrait: BK {
    override fun foo() = bar()
}

open abstract class K : KTrait {

}

class A : K() {
    override fun foo() = "A.foo"
    override fun bar() = "A.bar"

    inner class B : K() {
        override fun foo() = "B.foo"
        override fun bar() = "B.bar"

        fun test1() = super<K>@A.foo()
        fun test2() = super<K>@B.foo()
        fun test3() = super<K>.foo()
        fun test4() = super@A.foo()
        fun test5() = super@B.foo()
        fun test6() = super.foo()
    }
}


fun box(): String {
    val b = A().B()
    if (b.test1() != "A.bar") return "test1 ${b.test1()}"
    if (b.test2() != "B.bar") return "test2 ${b.test2()}"
    if (b.test3() != "B.bar") return "test3 ${b.test3()}"
    if (b.test4() != "A.bar") return "test4 ${b.test4()}"
    if (b.test5() != "B.bar") return "test5 ${b.test5()}"
    if (b.test6() != "B.bar") return "test6 ${b.test6()}"

    return "OK"
}

