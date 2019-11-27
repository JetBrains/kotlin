// IGNORE_BACKEND_FIR: JVM_IR
//inspired by kt3492
interface Base {
    val foo: String
    fun bar(): String
}

abstract class KWithOverride : Base {
    override val foo = bar()
}

abstract class K : KWithOverride() {

}

class A : K() {
    override val foo = "A.foo"
    override fun bar() = "A.bar"

    inner class B : K() {
        override val foo = "B.foo"
        override fun bar() = "B.bar"

        fun test1() = super<K>@A.foo
        fun test2() = super<K>@B.foo
        fun test3() = super<K>.foo
        fun test4() = super@A.foo
        fun test5() = super@B.foo
        fun test6() = super.foo
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
