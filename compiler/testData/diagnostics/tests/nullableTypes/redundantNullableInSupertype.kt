interface A
interface X: A<!NULLABLE_SUPERTYPE!>?<!><!REDUNDANT_NULLABLE!>?<!> {

}

fun interaction<T>(t: T) {
    if (t == null) {}

}