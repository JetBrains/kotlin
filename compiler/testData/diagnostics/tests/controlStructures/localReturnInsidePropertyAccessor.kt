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
            <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY, RETURN_NOT_ALLOWED!>return<!> 0
        }
    }

    fun some(): Int {
        return 6
    }
}