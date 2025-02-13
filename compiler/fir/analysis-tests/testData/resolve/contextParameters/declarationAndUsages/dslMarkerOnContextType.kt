// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74578
// LANGUAGE: +ContextParameters
context(ctx: T)
fun <T> implicit(): T = ctx

@DslMarker annotation class ExampleMarker

@ExampleMarker interface ExampleScope<A> {
    fun exemplify(): A
}

fun <A, T> withExampleReceiver(value: A, block: ExampleScope<A>.() -> T): T  = null!!

fun <A, T> withExampleContext(value: A, block: context(ExampleScope<A>) () -> T): T = null!!

context(a: ExampleScope<A>)
fun <A> similarExampleTo(other: A): A {
    return null!!
}

fun test1() {
    withExampleReceiver("string") {
        withExampleContext(true) {
            this.exemplify()
        }
    }
}

fun test2() {
    withExampleReceiver("string") {
        withExampleContext(true) {
            this@withExampleReceiver.exemplify()
        }
    }
}

fun test3() {
    withExampleContext(3) {
        withExampleReceiver("string") {
            <!DSL_SCOPE_VIOLATION!>implicit<!><ExampleScope<Int>>().exemplify()
        }
    }
}

fun test4() {
    withExampleContext(3) {
        withExampleReceiver("string") {
            similarExampleTo("string")
            <!DSL_SCOPE_VIOLATION!>similarExampleTo<!>(1)
        }
    }
}

fun test5() {
    withExampleReceiver("string") {
        withExampleContext(true) {
            similarExampleTo(true)
            <!DSL_SCOPE_VIOLATION!>similarExampleTo<!>("string")
        }
    }
}

fun test6() {
    withExampleContext("b") {
        withExampleContext(true) {
            <!DSL_SCOPE_VIOLATION!>similarExampleTo<!>("a")
            similarExampleTo(true)
        }
    }
}