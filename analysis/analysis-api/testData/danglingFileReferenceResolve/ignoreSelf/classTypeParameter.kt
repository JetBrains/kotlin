// COPY_RESOLUTION_MODE: IGNORE_SELF

class Foo<Value> {
    fun foo() {
        call<V<caret>alue>()
    }
}

fun <T> call(): T? {
    return null
}