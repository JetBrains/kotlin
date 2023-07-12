
fun <T> materialize(): T = TODO()

val a: () -> Unit = l@{
    // Expected type 'Unit' is used here for inference
    if (true) return@l materialize()

    // Expected type here is Unit, but it also implies coercion,
    // so we can end lambda body with statement
    if (true) 42
}

val b: () -> Unit = <!INITIALIZER_TYPE_MISMATCH!>l@{
    // Error, coercion can't be applied at this position!
    if (true) return@l "hello"

    // However, this is OK, because here coercion is applied
    "hello"
}<!>

val c: () -> Unit = {
    // Interesting enough, for such expessions we use expected type Unit
    // (compare that with the previous case, where we didn't used expected type Unit for "hello")
    materialize()
}
