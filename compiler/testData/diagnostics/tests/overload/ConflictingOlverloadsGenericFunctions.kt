<!CONFLICTING_OVERLOADS!>fun <T1> test1(x: List<T1>)<!> = x
<!CONFLICTING_OVERLOADS!>fun <T2> test1(x: List<T2>)<!> = x

<!CONFLICTING_OVERLOADS!>fun <T1> List<T1>.test1a()<!> {}
<!CONFLICTING_OVERLOADS!>fun <T2> List<T2>.test1a()<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> test2(x: List<T>)<!> = x
<!CONFLICTING_JVM_DECLARATIONS!>fun test2(x: List<String>)<!> = x

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> List<T>.test2a()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun List<String>.test2a()<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun <T : Any> test3(x: List<T>)<!> = x
<!CONFLICTING_JVM_DECLARATIONS!>fun test3(x: List<Any>)<!> = x

<!CONFLICTING_JVM_DECLARATIONS!>fun <T : Any> List<T>.test3a()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun List<Any>.test3a()<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> test4(x: Map<T, T>)<!> = x
<!CONFLICTING_JVM_DECLARATIONS!>fun <K, V> test4(x: Map<K, V>)<!> = x

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> Map<T, T>.test4a()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun <K, V> Map<K, V>.test4a()<!> {}

class Inv<T>

<!CONFLICTING_OVERLOADS!>fun <T> test5(x: Inv<T>)<!> = x
<!CONFLICTING_OVERLOADS!>fun <T> test5(x: Inv<out T>)<!> = x

fun <T> test6(x: Array<T>) = x
fun test6(x: Array<String>) = x

fun <T> test7(x: Inv<T>) = x
fun <T> Inv<T>.test7() {}