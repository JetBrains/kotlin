package foo

class A1 {
    val a: String by MyProperty1()
}

class MyProperty1 {}
fun MyProperty1.get(thisRef: Any?, desc: PropertyMetadata): String {
    throw Exception("$thisRef $desc")
}

//--------------------

class A2 {
    val a: String by MyProperty2()
}

class MyProperty2<T> {}
fun <T> MyProperty2<T>.get(thisRef: Any?, desc: PropertyMetadata): T {
    throw Exception("$thisRef $desc")
}

//--------------------

class A3 {
    val a: String by MyProperty3()

    class MyProperty3<T> {}

    fun <T> MyProperty3<T>.get(thisRef: Any?, desc: PropertyMetadata): T {
        throw Exception("$thisRef $desc")
    }
}