fun foo(first: Boolean, second: Boolean, third: Boolean): Int {
    if (first) {
        return 4
    } else if (second) {
        val x = 3
        return x + 2
    } else {
        // Yet we don't flatten this 'if', it matches the current PSI2IR behavior
        if (third) {
            return 0
        } else return -1
    }
}

fun example() {
    val a = if (true) true else false
    val b = if (true) else false
    val c = if (true) true
    val d = if (true) true else;
    val e = if (true) {} else false
    val f = if (true) true else {}

    {
        if (true) true
    }();

    {
        if (true) true else false
    }();

    {
        if (true) {} else false
    }();


    {
        if (true) true else {}
    }()

    fun t(): Boolean {
        return if (true) true
    }

    return if (true) true else {}
}
