<!CONFLICTING_OVERLOADS!>fun <T1> test1(x: List<T1>)<!> = x
<!CONFLICTING_OVERLOADS!>fun <T2> test1(x: List<T2>)<!> = x

<!CONFLICTING_OVERLOADS!>fun <T1> List<T1>.test1a()<!> {}
<!CONFLICTING_OVERLOADS!>fun <T2> List<T2>.test1a()<!> {}

fun <T> test2(x: List<T>) = x
fun test2(x: List<String>) = x

fun <T> List<T>.test2a() {}
fun List<String>.test2a() {}

fun <T : Any> test3(x: List<T>) = x
fun test3(x: List<Any>) = x

fun <T : Any> List<T>.test3a() {}
fun List<Any>.test3a() {}

fun <T> test4(x: Map<T, T>) = x
fun <K, V> test4(x: Map<K, V>) = x

fun <T> Map<T, T>.test4a() {}
fun <K, V> Map<K, V>.test4a() {}

class Inv<T>

<!CONFLICTING_OVERLOADS!>fun <T> test5(x: Inv<T>)<!> = x
<!CONFLICTING_OVERLOADS!>fun <T> test5(x: Inv<out T>)<!> = x

fun <T> test6(x: Array<T>) = x
fun test6(x: Array<String>) = x

fun <T> test7(x: Inv<T>) = x
fun <T> Inv<T>.test7() {}
