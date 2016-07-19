// See also: KT-6611 (cast can never succeed: Class<T> -> Class<Any>)

class Class<T>(val name: String, val instance: T)

fun <T> test(clazz: Class<T>) {
    println((<!UNCHECKED_CAST!>clazz as Class<Any><!>).name)
}

fun use() {
    test(Class("String", ""))
}

fun checkArrays(): Array<Any> {
    val someArray = arrayOfNulls<Any>(5)
    someArray <!CAST_NEVER_SUCCEEDS!>as<!> Array<Int>
    return <!UNCHECKED_CAST!>someArray as Array<Any><!>
}

class Wrapper<T>(val x: T)

fun checkArrays2(): Array<Wrapper<String>> {
    val someArray = arrayOf(Wrapper(1), Wrapper(2))
    return <!UNCHECKED_CAST!>someArray as Array<Wrapper<String>><!>
}

fun println(s: String) = s

