// ISSUE: KT-41939

// FILE: Ann.java

public @interface Ann {
    String value()
}

// FILE: main.kt

fun test(ann: Ann) {
    ann.value
    ann.<!UNRESOLVED_REFERENCE!>value<!>() // should be an error
}
