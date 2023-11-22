// COPY_RESOLUTION_MODE: PREFER_SELF

class Foo<Value> {
    fun foo() {
        call<V<caret>alue>()
    }
}

fun <T> call(): T? {
    return null
}