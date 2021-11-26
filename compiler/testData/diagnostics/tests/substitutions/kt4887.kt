// FIR_IDENTICAL
package h

public class MyClass<S, T>(param: MyClass<S, T>) {
    fun test() {
        val result: MyClass<Any, Any>? = null
        MyClass<S, Any>(result <!UNCHECKED_CAST!>as MyClass<S, Any><!>)
    }
}
