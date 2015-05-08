// "Remove 'out' modifier" "true"
fun foo<T>(x : T) {}

fun bar() {
    foo<<caret>out Int>(44)
}
