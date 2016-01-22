// DISABLE-ERRORS
class A
class B

fun <T> foo(klass: Class<T>) {

}

fun <T> bar(klass: Class<T>) {

}

fun main() {
    foo(<selection>A::class.java</selection>)
    foo(B::class.java)
    bar(B::class.java)
    bar(A::class.java)
    javaClass()
}