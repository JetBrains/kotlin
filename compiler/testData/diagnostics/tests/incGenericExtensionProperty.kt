// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-70835
// FIR_DUMP

interface Context {
    var <V> Variable<V>.value: V
}

class Variable<T>

abstract class A : Context {
    abstract val intVar: Variable<Int>

    fun foo() {
        intVar.value++
    }
}
