class A {
    fun foo() {}
}
fun bar() {}
val qux = 42

val test1 = A::class
val test2 = qux::class
val test3 = A::foo
val test4 = ::A
val test5 = A()::foo
val test6 = ::bar
val test7 = ::qux