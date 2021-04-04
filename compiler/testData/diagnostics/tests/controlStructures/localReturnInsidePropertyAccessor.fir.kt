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
            // bug: return's target is the function g
            <!RETURN_NOT_ALLOWED!>return<!> <!RETURN_TYPE_MISMATCH!>0<!>
        }
    }

    fun some(): Int {
        return 6
    }
}
