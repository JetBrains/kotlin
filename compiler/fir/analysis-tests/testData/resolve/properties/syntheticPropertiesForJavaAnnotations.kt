// ISSUE: KT-41939

// FILE: Ann.java

public @interface Ann {
    String value();
}

// FILE: main.kt

fun test(ann: Ann) {
    ann.value
    ann.<!FUNCTION_EXPECTED!>value<!>() // should be an error
}
