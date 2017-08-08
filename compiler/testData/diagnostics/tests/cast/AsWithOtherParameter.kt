// See also: KT-6611 (cast can never succeed: Class<T> -> Class<Any>)

class Class<T>(val name: String, val instance: T)

fun <T> test(clazz: Class<T>) {
    println((clazz <!UNCHECKED_CAST!>as Class<Any><!>).name)
}

fun use() {
    test(Class("String", ""))
}

fun checkArrays(): Array<Any> {
    val someArray = arrayOfNulls<Any>(5)
    someArray <!UNCHECKED_CAST!>as Array<Int><!>
    return someArray <!UNCHECKED_CAST!>as Array<Any><!>
}

class Wrapper<T>(val x: T)

fun checkArrays2(): Array<Wrapper<String>> {
    val someArray = arrayOf(Wrapper(1), Wrapper(2))
    return someArray <!UNCHECKED_CAST!>as Array<Wrapper<String>><!>
}

fun checkArrays3() {
    val someArray = arrayOfNulls<String>(1)
    someArray <!UNCHECKED_CAST!>as Array<Any><!>
    val intArray = arrayOfNulls<Int>(1)
    intArray <!UNCHECKED_CAST!>as Array<Any><!>
}

fun println(s: String) = s

