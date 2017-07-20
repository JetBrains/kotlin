// WITH_RUNTIME
fun foo(a: Any) {
    <caret>if (a == "") {
        println(a)
    }
    else if (a is String) {
        println(a)
    }
    else if (a is List<*>) {
        @Suppress("UNCHECKED_CAST")
        println(a as List<String>)
    }
}