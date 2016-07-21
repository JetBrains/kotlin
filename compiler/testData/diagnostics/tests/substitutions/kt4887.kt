package h

public class MyClass<S, T>(<!UNUSED_PARAMETER!>param<!>: MyClass<S, T>) {
    fun test() {
        val result: MyClass<Any, Any>? = null
        MyClass<S, Any>(<!UNCHECKED_CAST!>result as MyClass<S, Any><!>)
    }
}