fun main(args: Array<String>) {
    val b: A = C()
    <caret>val a = 1
}

open class A {
    fun funA() {}
    private fun funAp() {}
}

open class B: A() {
    fun funB() {}
    private fun funBp() {}
}

class C: B() {
    fun funC() {}
    private fun funCp() {}
}

// INVOCATION_COUNT: 2
// EXIST: funA, funAp, funB, funBp, funC, funCp
// NOTHING_ELSE

// RUNTIME_TYPE: C
