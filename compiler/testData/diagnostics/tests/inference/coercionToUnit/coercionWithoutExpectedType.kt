// FIR_IDENTICAL
fun <T> materialize(): T = TODO()

fun implicitCoercion() {
    val a = {
        // Block is implicitly Unit-coerced, so it is allowed to place statement at the end of lambda
        if (true) 42
    }

    val b = l@{
        return@l
    }

    val c = l@{
        // Error: block doesn't have an expected type, so call can't be inferred!
        return@l <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    }
}
