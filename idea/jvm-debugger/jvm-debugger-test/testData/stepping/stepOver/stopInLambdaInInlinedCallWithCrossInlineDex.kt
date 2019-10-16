// FILE: stopInLambdaInInlinedCallWithCrossInlineDex.kt
// EMULATE_DEX: true
// KT-15282

package stopInLambdaInInlinedCallWithCrossInlineDex

fun main(args: Array<String>) {
    foo {
        //Breakpoint!
        12
    }
}

fun bar(f: () -> Unit) {
    f()
}

inline fun foo(crossinline func: () -> Int) {
    bar {
        func()
    }
}