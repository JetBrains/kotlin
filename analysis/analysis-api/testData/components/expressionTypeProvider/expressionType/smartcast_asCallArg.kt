fun test(a: Any) {
    if (a is String) {
        takeString(<expr>a</expr>)
    }
}

fun takeString(s: String) {}