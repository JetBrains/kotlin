// DISABLE-ERRORS
class A
class B

fun foo<T>(klass: Class<T>) {

}

fun bar<T>(klass: Class<T>) {

}

fun main() {
    foo(<selection>javaClass<A>()</selection>)
    foo(javaClass<B>())
    bar(javaClass<B>())
    bar(javaClass<A>())
    javaClass()
}