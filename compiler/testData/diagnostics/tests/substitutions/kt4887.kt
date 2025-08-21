// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package h

public class MyClass<S, T>(param: MyClass<S, T>) {
    fun test() {
        val result: MyClass<Any, Any>? = null
        MyClass<S, Any>(result <!UNCHECKED_CAST!>as MyClass<S, Any><!>)
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, localProperty, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
