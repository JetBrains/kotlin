interface A
interface X: A<!NULLABLE_SUPERTYPE!>?<!><!REDUNDANT_NULLABLE!>?<!> {

}

fun <T> interaction(t: T) {
    if (t == null) {}

}