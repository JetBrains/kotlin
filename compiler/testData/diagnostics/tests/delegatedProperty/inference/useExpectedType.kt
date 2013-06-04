package foo

class A1 {
    var a1: String by MyProperty1()
    var b1: String by getMyProperty1()
}

var c1: String by getMyProperty1<Nothing?, String>()

fun getMyProperty1<A, B>() = MyProperty1<A, B>()

class MyProperty1<R, T> {

    public fun get(thisRef: R, desc: PropertyMetadata): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }

    public fun set(thisRef: R, desc: PropertyMetadata, value: T) {
        println("set $thisRef ${desc.name} $value")
    }
}

//--------------------------

class A2 {
    var a2: String by MyProperty2()
    var b2: String by getMyProperty2()
}

fun getMyProperty2<A>() = MyProperty2<A>()

class MyProperty2<T> {

    public fun get(thisRef: Any?, desc: PropertyMetadata): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }

    public fun set(thisRef: Any?, desc: PropertyMetadata, value: T) {
        println("set $thisRef ${desc.name} $value")
    }
}

//--------------------------

class A3 {
    var a3: String by MyProperty3()
    var b3: String by getMyProperty3()
}

fun getMyProperty3<A>() = MyProperty3<A>()

class MyProperty3<T> {

    public fun get(thisRef: T, desc: PropertyMetadata): String {
        println("get $thisRef ${desc.name}")
        return ""
    }

    public fun set(thisRef: Any?, desc: PropertyMetadata, value: T) {
        println("set $thisRef ${desc.name} $value")
    }
}

//--------------------------

class A4 {
    val a4: String by MyProperty4()
    val b4: String by getMyProperty4()
}

fun getMyProperty4<A, B>() = MyProperty4<A, B>()

class MyProperty4<R, T> {

    public fun get(thisRef: R, desc: PropertyMetadata): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }
}

//--------------------------
fun println(a: Any?) = a