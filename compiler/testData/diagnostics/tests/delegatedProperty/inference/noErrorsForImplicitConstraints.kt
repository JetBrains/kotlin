package foo

class A {
    var a5: String by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyProperty1<!>()
    var b5: String by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getMyProperty1<!>()
}

fun <A, B> getMyProperty1() = MyProperty1<A, B>()

class MyProperty1<T, R> {

    public fun getValue(thisRef: R, desc: PropertyMetadata): T {
        throw Exception()
    }

    public fun setValue(i: Int, j: Int, k: Int) {
        println("set")
    }
}

// -----------------

class B {
    var a5: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>MyProperty2()<!>
    var b5: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>getMyProperty2()<!>
}

fun <A, B> getMyProperty2() = MyProperty2<A, B>()

class MyProperty2<T, R> {

    public fun getValue(thisRef: R, desc: PropertyMetadata): T {
        throw Exception()
    }

    public fun setValue(i: Int) {
        println("set")
    }
}

// -----------------
fun println(a: Any?) = a