fun main() {
    val b: Boolean? = null
    if (b != null) {
        if (!b) {} // OK
        if (b) {} // Error: Condition must be of type kotlin.Boolean, but is of type kotlin.Boolean?
        if (b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {} // WARN: Unnecessary non-null assertion (!!) on a non-null receiver of type kotlin.Boolean?
        foo(b) // OK
    }
}

fun foo(a: Boolean) = a
