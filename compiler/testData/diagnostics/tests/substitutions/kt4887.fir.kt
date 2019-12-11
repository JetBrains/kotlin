package h

public class MyClass<S, T>(param: MyClass<S, T>) {
    fun test() {
        val result: MyClass<Any, Any>? = null
        MyClass<S, Any>(result as MyClass<S, Any>)
    }
}