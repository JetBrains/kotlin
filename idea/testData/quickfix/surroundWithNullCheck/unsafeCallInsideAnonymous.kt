// "Surround with null check" "true"
// WITH_RUNTIME

fun foo(arg: Int?) {
    run(fun() = arg<caret>.hashCode())
}