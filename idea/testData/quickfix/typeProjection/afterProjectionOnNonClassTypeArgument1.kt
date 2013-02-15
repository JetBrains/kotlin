// "Remove 'out' modifier" "true"
fun foo<T>(x : T) {}

fun bar() {
    foo<Int>(44)
}
