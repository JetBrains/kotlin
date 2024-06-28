// FIR_IDENTICAL
<!CONFLICTING_JVM_DECLARATIONS!>fun <T> test2(x: List<T>) = x<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun test2(x: List<String>) = x<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> List<T>.test2a() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun List<String>.test2a() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T : Any> test3(x: List<T>) = x<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun test3(x: List<Any>) = x<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T : Any> List<T>.test3a() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun List<Any>.test3a() {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> test4(x: Map<T, T>) = x<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <K, V> test4(x: Map<K, V>) = x<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun <T> Map<T, T>.test4a() {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun <K, V> Map<K, V>.test4a() {}<!>

class Inv<T>

fun <T> test6(x: Array<T>) = x
fun test6(x: Array<String>) = x

fun <T> test7(x: Inv<T>) = x
fun <T> Inv<T>.test7() {}
