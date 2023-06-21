// FILE: Some.java

public class Some extends Base {
    public Some(Object o) {
        super(o);
    }
}

// FILE: test.kt

open class Base(internal val foo: Any) {
    fun <T> getFoo(): T = foo <!UNCHECKED_CAST!>as T<!>
}

fun bar(some: Some) {
    val foo = some.foo
    val baz = some.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>foo<!><String>

    val getFoo = some.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getFoo<!>()
    val getBaz = some.getFoo<String>()
}
