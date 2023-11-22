// COPY_RESOLUTION_MODE: IGNORE_SELF

fun <Value> foo() {
    call<V<caret>alue>()
}

fun <T> call(): T? {
    return null
}