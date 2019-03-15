// "Surround with null check" "true"
// WITH_RUNTIME

fun Int.bar() = this

fun foo(arg: Int?) {
    run(fun() = arg<caret>.bar())
}