// "Replace javaClass<T>() with T::class in whole project" "true"
// WITH_RUNTIME

Ann(List::class) class MyClass1
Ann(List::class) class MyClass2
Ann(MutableList::class) class MyClass3

Ann(Array<String>::class) class MyClass4
Ann(Array<*>::class) class MyClass5
Ann(Array<in String>::class) class MyClass6

class Outer<T> {
    Ann(Array<T>::class)
    class Nested
}
