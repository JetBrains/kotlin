// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77301
// LANGUAGE: +ContextParameters
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class MyMarker

@MyMarker
class DslReceiver

@MyMarker
class DslReceiver2

class SomeOtherClass

fun foo1(block: context(SomeOtherClass) DslReceiver.() -> Unit) {}
fun foo2(block: context(DslReceiver) SomeOtherClass.() -> Unit) {}
fun foo3(block: context(SomeOtherClass, DslReceiver) () -> Unit) {}
fun foo4(block: context(DslReceiver, SomeOtherClass) () -> Unit) {}

context(c: SomeOtherClass)
val nonDslVal: SomeOtherClass
    get() = c

context(c: DslReceiver)
val dslVal: DslReceiver
    get() = c

fun test() {
    foo1 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
    foo2 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
    foo3 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
    foo4 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
}

fun annotatedFunctionType(block: @MyMarker context(String) () -> Unit) {}
fun annotatedFunctionType2(block: @MyMarker context(Int) () -> Unit) {}

context(_: String)
fun stringFromContext() {}

fun test2() {
    annotatedFunctionType(block = {
        annotatedFunctionType2(block = {
            <!DSL_SCOPE_VIOLATION!>stringFromContext<!>()
        })
    })
}
