trait X: Any<!NULLABLE_SUPERTYPE!>?<!><!REDUNDANT_NULLABLE!>?<!> {

}

fun interaction<T>(t: T) {
    if (t == null) {}

}