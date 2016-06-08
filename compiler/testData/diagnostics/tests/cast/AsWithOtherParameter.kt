// See also: KT-6611 (cast can never succeed: Class<T> -> Class<Any>)

class Class<T>(val name: String, val instance: T)

fun <T> test(clazz: Class<T>) {
    println((<!UNCHECKED_CAST!>clazz as Class<Any><!>).name)
}

fun use() {
    test(Class("String", ""))
}

fun println(s: String) = s

