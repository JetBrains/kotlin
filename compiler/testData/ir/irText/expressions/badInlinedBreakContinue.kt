// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// !IGNORE_ERRORS
// DIAGNOSTICS: -NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER -BREAK_OR_CONTINUE_OUTSIDE_A_LOOP -UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE -NO_VALUE_FOR_PARAMETER -NOT_A_LOOP_LABEL -UNRESOLVED_REFERENCE -TYPE_MISMATCH -BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY
// WITH_STDLIB

inline fun foo(block: () -> Unit) { block() }

inline fun bar(block1: () -> Unit, noinline block2: () -> Unit) {
    block1()
    block2()
}

inline fun baz(crossinline block: () -> Unit) { block() }

inline fun <T> Iterable<T>.myForEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}

fun test1() {
    {break}()
    {continue}()

    (fun () {break})()
    (fun () {continue})()

    foo {break}
    foo {continue}

    foo(fun () {break})
    foo(fun () {continue})
}

fun test2() {
    L1@ while (true) {
        {break@ERROR}()
        {continue@ERROR}()

        (fun () {break@ERROR})()
        (fun () {continue@ERROR})()

        foo {break@ERROR}
        foo {continue@ERROR}

        foo(fun () {break@ERROR})
        foo(fun () {continue@ERROR})
    }
}

fun test3() {
    L1@ while (true) {
        val lambda = {
            {break@L1}()
            {continue@L1}()

            (fun () {break@L1})()
            (fun () {continue@L1})()

            foo {break@L1}
            foo {continue@L1}

            foo(fun () {break@L1})
            foo(fun ()  {continue@L1})
        }
    }
}

fun test4() {
    while ({ break }()) {
    }
    while ({ continue }()) {
    }

    while ((fun() { break })()) {
    }
    while ((fun() { continue })()) {
    }

    while (foo { break }) {
    }
    while (foo { continue }) {
    }

    while (foo(fun() { break })) {
    }
    while (foo(fun() { continue })) {
    }
}

fun test5() {
    listOf(1, 2, 3).forEach { i -> if (i == 2) break }
    listOf(1, 2, 3).forEach { i -> if (i == 2) continue }
    listOf(1, 2, 3).forEach(fun (i: Int) { if (i == 2) break })
    listOf(1, 2, 3).forEach(fun (i: Int) { if (i == 2) continue })
}

fun test6() {
    while (true) {
        bar({}, {break})
        bar({}, {continue})

        bar(fun () {}, fun () {break})
        bar(fun () {}, fun () {continue})
    }
}

fun test7() {
    (1..3).myForEach { i ->
        if (i == 2) {
            break
        }
    }
}

fun test8() {
    while (true) {
        baz({break})
    }
}
