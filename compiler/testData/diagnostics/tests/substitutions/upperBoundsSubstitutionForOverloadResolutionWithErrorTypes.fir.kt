// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS -UNUSED_PARAMETER
fun <T> f1(l: List1<T>): T {throw Exception()} // ERROR type here
fun <T> f1(l: List2<T>): T {throw Exception()} // ERROR type here
fun <T> f1(c: Collection<T>): T{throw Exception()}

fun <T> test(l: List<T>) {
    f1(l)
}