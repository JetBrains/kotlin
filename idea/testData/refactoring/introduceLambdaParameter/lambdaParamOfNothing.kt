// WITH_DEFAULT_VALUE: false
fun foo(a: Int) {
    <selection>throw Exception("Error: $a")</selection>
}

fun test() {
    foo(1)
}