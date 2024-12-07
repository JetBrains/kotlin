// RUN_PIPELINE_TILL: BACKEND
//KT-5182 Data flow info is lost for 'when' branches

open class A

class B: A() {
    fun foo() = 1
}

fun foo(a: A) = when {
    a !is B -> 2
    true -> a.foo() //'foo' is unresolved, smart cast doesn't work
    else -> a.foo()
}