package foo

class A {
    var a5: String by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty1<!>()
    var b5: String by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getMyProperty1<!>()
}

fun getMyProperty1<A, B>() = MyProperty1<A, B>()

class MyProperty1<T, R> {

    public fun get(thisRef: R, desc: PropertyMetadata): T {
        throw Exception()
    }

    public fun set(i: Int, j: Int, k: Int) {
        println("set")
    }
}

// -----------------

class B {
    var a5: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>MyProperty2()<!>
    var b5: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>getMyProperty2()<!>
}

fun getMyProperty2<A, B>() = MyProperty2<A, B>()

class MyProperty2<T, R> {

    public fun get(thisRef: R, desc: PropertyMetadata): T {
        throw Exception()
    }

    public fun set(i: Int) {
        println("set")
    }
}

// -----------------
fun println(a: Any?) = a