package foo

class A1 {
    val a1: String by MyProperty1()
    val b1: String by getMyProperty1()
}

val c1: String by getMyProperty1()
val d1: String by MyProperty1()

fun getMyProperty1<A, B>() = MyProperty1<A, B>()

class MyProperty1<R, T> {

    public fun get(thisRef: R, desc: PropertyMetadata): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }
}

//--------------------------

class A2 {
    val a2: String by MyProperty2()
    val b2: String by getMyProperty2()
}

val c2: String by getMyProperty2()
val d2: String by MyProperty2()

fun getMyProperty2<A>() = MyProperty2<A>()

class MyProperty2<T> {

    public fun get(thisRef: Any?, desc: PropertyMetadata): T {
        println("get $thisRef ${desc.name}")
        throw Exception()
    }
}

//--------------------------

class A3 {
    val a3: String by MyProperty3()
    val b3: String by getMyProperty3()
}

val c3: String by getMyProperty3()
val d3: String by MyProperty3()

fun getMyProperty3<A>() = MyProperty3<A>()

class MyProperty3<T> {

    public fun get(thisRef: T, desc: PropertyMetadata): String {
        println("get $thisRef ${desc.name}")
        return ""
    }
}

//--------------------------
fun println(a: Any?) = a