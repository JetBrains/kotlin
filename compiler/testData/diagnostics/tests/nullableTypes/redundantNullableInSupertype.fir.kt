interface A
interface X: A?<!NULLABLE_SUPERTYPE!>?<!> {

}

fun <T> interaction(t: T) {
    if (t == null) {}

}