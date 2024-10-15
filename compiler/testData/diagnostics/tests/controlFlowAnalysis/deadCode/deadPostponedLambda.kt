// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun throwException(): Nothing = throw RuntimeException()

fun case1(lambda: () -> Unit) {}

fun <T> case2(lambda: () -> T): () -> T { return null!! }

fun <T> case3(lambda1: Any, lambda2: T): T {
    return lambda2
}

fun outer(a: () -> Int) {}

fun test1() {
    throwException()
    <!UNREACHABLE_CODE!>val a = case1 { val a = 0 ; val b = 0 }<!>
}

fun test2() {
    throwException()
    <!UNREACHABLE_CODE!>val a = case1(
        fun() {
            val a = 0 ; val b = 0
        }
    )<!>
}

fun test3(){
    throwException()
    <!UNREACHABLE_CODE!>val a = case2 { 1 }<!>
}

fun test4(){
    throwException()
    <!UNREACHABLE_CODE!>val a = case2 (
        fun() = 1
    )<!>
}

fun test5(){
    throwException()
    <!UNREACHABLE_CODE!>outer(
        case2(
            { 1 }
        )
    )<!>
}

fun test6(){
    throwException()
    <!UNREACHABLE_CODE!>outer(
        case2(
            fun() = 1
        )
    )<!>
}


fun test7() {
    throwException()
    <!UNREACHABLE_CODE!>val a = case3(
        fun() {},
        fun() = 1,
    )<!>
}

fun test8() {
    throwException()
    <!UNREACHABLE_CODE!>val a = case3(
        { 1 },
        { val a = 0 }
    )<!>
}

fun test9() {
    throwException()
    <!UNREACHABLE_CODE!>outer(
        case3(
            fun() {},
            fun() = 1,
        )
    )<!>
}

fun test10() {
    throwException()
    <!UNREACHABLE_CODE!>outer(
        case3(
            { },
            { 1 }
        )
    )<!>
}

fun test11() {
    throwException()
    <!UNREACHABLE_CODE!>case3(
        case3(
            fun() {},
            fun() {},
        ),
        fun() {}
    )<!>
}

fun test12() {
    throwException()
    <!UNREACHABLE_CODE!>case3(
        case3(
            {},
            {},
        ),
        {}
    )<!>
}
