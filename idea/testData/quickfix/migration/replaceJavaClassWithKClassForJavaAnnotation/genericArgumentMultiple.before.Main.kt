// "Replace javaClass<T>() with T::class in whole project" "true"
// WITH_RUNTIME

Ann(javaClass<List<String>><caret>()) class MyClass1
Ann(javaClass<List<*>>()) class MyClass2
Ann(javaClass<MutableList<in String>>()) class MyClass3

Ann(javaClass<Array<String>>()) class MyClass4
Ann(javaClass<Array<*>>()) class MyClass5
Ann(javaClass<Array<in String>>()) class MyClass6

class Outer<T> {
    Ann(javaClass<Array<T>>())
    class Nested
}
