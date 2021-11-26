fun foo() {
    return/* foo@(1,1) */
}

fun bar(): String {
    return/* bar@(5,1) */""
}

fun baz(): Int {
    if (true) {
        return/* baz@(9,1) */1
    } else {
        return/* baz@(9,1) */2
    }
}

fun quux(): Int {
    while(true) {
        return/* quux@(17,1) */1
    }
    return/* quux@(17,1) */2
}

fun test() {
    run {
        return/* test@(24,1) */
    }
    fun() {
        return/* null@(28,5) */
    }
    run {
        with("") {
            return/* test@(24,1) */
        }
    }
}
