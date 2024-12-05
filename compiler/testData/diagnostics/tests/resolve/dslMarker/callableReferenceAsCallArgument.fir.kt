// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73693

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Dsl

abstract class A {
    fun aFun() {}
}

class ADerived : A() {
    fun aDerivedFun() {}
}

class B

fun myRunFun(x: () -> Unit) {}

fun foo(x: (@Dsl A).() -> Unit) {}
fun bar(x: (@Dsl B).() -> Unit) {}

fun main() {
    foo {
        myRunFun(::aFun)

        if (this is ADerived) {
            myRunFun(::aDerivedFun)
        }

        bar {
            myRunFun(::aFun)

            if (this@foo is ADerived) {
                myRunFun(::aDerivedFun)
            }
        }
    }
}
