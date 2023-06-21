// FILE: Some.java

public class Some extends Base {
    public Some(Object o) {
        super(o);
    }
}

// FILE: test.kt

open class Base(val x: Any) {
    fun <T> getFoo(): T = x <!UNCHECKED_CAST!>as T<!>
}

fun bar(some: Some) {
    val foo = some.<!UNRESOLVED_REFERENCE!>foo<!>
    val baz = some.<!UNRESOLVED_REFERENCE!>foo<!><String>

    val getFoo = some.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getFoo<!>()
    val getBaz = some.getFoo<String>()
}
