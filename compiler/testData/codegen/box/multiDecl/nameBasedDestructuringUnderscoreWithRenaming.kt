// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// Verifies that property is called when name-based destructuring uses '_' as name

object O {
    var counter = 0

    val first: Int
        get() = counter++
}

fun box(): String {
    val (_ = first) = O
    (val _ = first) = O

    for ((_ = first) in arrayOf(O)) {}
    for ((val _ = first) in arrayOf(O)) {}

    fun foo(f: (O) -> Unit) = f(O)

    foo { (_ = first) -> }
    foo { (val _ = first) -> }

    return if (O.counter == 6) "OK" else "FAIL: ${O.counter}"
}