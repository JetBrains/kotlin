// IS_APPLICABLE: false

inline fun test() {
    foo <caret>{ "" }
}

fun foo(f: (Int) -> String) {}