// MEMBER_NAME_FILTER: result
// RESOLVE_PROPERTY_PART: GETTER
// IGNORE_FIR
interface A<T> {
    var result
        get() = get()
        set(value) {}

    fun get(): T? = null
}

class <caret>B(a: A<String>): A<String> by a