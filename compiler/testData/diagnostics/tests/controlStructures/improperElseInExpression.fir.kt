// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

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