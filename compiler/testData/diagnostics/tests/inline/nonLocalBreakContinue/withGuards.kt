// LANGUAGE: +BreakContinueInInlineLambdas, +WhenGuards
// ISSUE: KT-1436

fun noInline(s: ()->Unit) {
    s()
}

inline fun inline(s: ()->Unit) {
    s()
}

fun test1(x: Any, b: Boolean?){
    for (i in 0..10) {
        inline {
            when(x) {
                is String <!UNSUPPORTED_FEATURE!>if b ?: continue<!> -> 1
                is Int <!UNSUPPORTED_FEATURE!>if b ?: break<!> -> 2
            }
        }
    }
}

fun test2(x: Any, b: Boolean?){
    for (i in 0..10) {
        noInline {
            when(x) {
                is String <!UNSUPPORTED_FEATURE!>if b ?: continue<!> -> 1
                is Int <!UNSUPPORTED_FEATURE!>if b ?: break<!> -> 2
            }
        }
    }
}

fun test3(x: Any) {
    for (i in 0..10) {
        inline {
            when(x) {
                is String <!UNSUPPORTED_FEATURE!>if i == 1 || continue<!> -> 1
                else <!UNSUPPORTED_FEATURE!>if i == 2 && break<!> -> 2
            }
        }
    }
}

fun test4(x: Any) {
    for (i in 0..10) {
        noInline {
            when(x) {
                is String <!UNSUPPORTED_FEATURE!>if i == 1 || continue<!> -> 1
                else <!UNSUPPORTED_FEATURE!>if i == 2 && break<!> -> 2
            }
        }
    }
}