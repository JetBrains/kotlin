// LANGUAGE: +BreakContinueInInlineLambdas
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// !IGNORE_ERRORS

inline fun foo(block: () -> Unit) { block() }

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