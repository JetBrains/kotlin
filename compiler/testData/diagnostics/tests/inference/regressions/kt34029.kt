// RUN_PIPELINE_TILL: FRONTEND
open class MyClass<T> {
    object MyObject : MyClass<Boolean>() { }
}

val foo1 = MyClass.MyObject // it's ok
val foo2 = <!FUNCTION_CALL_EXPECTED!>MyClass<Boolean><!>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>MyObject<!> // here's stofl
