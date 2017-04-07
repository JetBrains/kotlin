interface ClassData

fun f() = object : ClassData {
    val someInt: Int
        get() {
            return 5
        }
}

fun g() = object : ClassData {
    init {
        if (true) {
            <!RETURN_NOT_ALLOWED, RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 0
        }
    }

    fun some(): Int {
        return 6
    }
}