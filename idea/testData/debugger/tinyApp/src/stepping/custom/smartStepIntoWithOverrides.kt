package smartStepIntoWithOverrides

abstract class A {
    abstract fun foo(): String
}

open class B : A() {
    override fun foo(): String = "B"
}

open class C : B() {
    override fun foo(): String = "C"
}

class D : C()

fun test1() {
    val d: D = D()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    d.foo()                                  // 12
}

fun test2() {
    val a: A = B()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    a.foo()                                  // 8
}

fun test3() {
    val a: A = C()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    a.foo()                                  // 12
}

fun test4() {
    val a: A = B()
    val d: D = D()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    a.foo() + d.foo()                        // 8
}

fun test5() {
    val a: A = B()
    val d: D = D()
    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    a.foo() + d.foo()                        // 12 (Shouldn't stop at B.foo() even it's evaluated before C.foo())
}

fun main(args: Array<String>) {
    test1()
    test2()
    test3()
    test4()
    test5()
}

