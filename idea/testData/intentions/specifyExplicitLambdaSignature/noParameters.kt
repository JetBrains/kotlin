// WITH_RUNTIME

fun bar() {}

fun foo() {
    run <caret>{
        bar()
    }
}