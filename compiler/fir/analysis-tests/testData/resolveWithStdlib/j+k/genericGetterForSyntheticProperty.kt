// FIR_IDENTICAL
// FILE: Some.java

public class Some {
    public <T> T getFoo() {
        return null;
    }
}

// FILE: test.kt

fun bar(some: Some) {
    val foo = some.<!UNRESOLVED_REFERENCE!>foo<!>
    val baz = some.<!UNRESOLVED_REFERENCE!>foo<!><String>

    val getFoo = some.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getFoo<!>()
    val getBaz = some.getFoo<String>()
}
