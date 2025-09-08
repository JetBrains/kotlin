// RUN_PIPELINE_TILL: FRONTEND
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

    val getFoo = some.<!CANNOT_INFER_PARAMETER_TYPE!>getFoo<!>()
    val getBaz = some.getFoo<String>()
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, javaType, localProperty, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
