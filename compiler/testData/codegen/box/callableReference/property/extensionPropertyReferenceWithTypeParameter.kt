// WITH_STDLIB

var result = "failed"

val <T> List<T>.foo: T
    get() = "not" as T

val <T> List<T>.bar: T.()-> String
    get() = { "fai" }

val <T> (List<T>.()-> T).baz: T
    get() = this(listOf())

fun box(): String {
    fun <T> test(): List<T>.()-> T = List<T>::foo
    result = test<String>()(listOf())

    fun <T> test2(): List<T>.() -> (T.() -> String) = List<T>::bar
    result += test2<String>()(listOf())("")

    fun <T> List<T>.foo():T { return "led" as T }
    fun <T> test3():() -> T = List<T>::foo::baz
    result += test3<String>()()

    return if (result == "notfailed") "OK"
    else "fail"
}