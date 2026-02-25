// RUN_PIPELINE_TILL: FRONTEND
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
    val baz = some.foo<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><String><!>

    val getFoo = some.<!CANNOT_INFER_PARAMETER_TYPE!>getFoo<!>()
    val getBaz = some.getFoo<String>()
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, javaType, localProperty, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
