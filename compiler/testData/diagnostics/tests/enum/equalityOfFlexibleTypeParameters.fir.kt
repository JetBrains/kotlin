// FILE: A.java

class MyHelpers {
    public static <T> T id(T it) {
        return it;
    }
}

// FILE: B.kt

fun <<!CONFLICTING_UPPER_BOUNDS, INCONSISTENT_TYPE_PARAMETER_BOUNDS, MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T: <!FINAL_UPPER_BOUND!>Int<!><!>, <!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>K: Any?<!>> mest(a: T, b: K) where T : <!FINAL_UPPER_BOUND, ONLY_ONE_CLASS_BOUND_ALLOWED!>String<!>, K: <!FINAL_UPPER_BOUND, ONLY_ONE_CLASS_BOUND_ALLOWED!>Boolean<!> {
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>MyHelpers.id(a) === MyHelpers.id(b)<!>
}
