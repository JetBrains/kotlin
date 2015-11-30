// DISABLE-ERRORS
class A
class B

fun <T> foo(klass: Class<T>) {

}

fun <T> bar(klass: Class<T>) {

}

fun main() {
    foo(<selection>javaClass<A>()</selection>)
    foo(javaClass<B>())
    bar(javaClass<B>())
    bar(javaClass<A>())
    javaClass()
}