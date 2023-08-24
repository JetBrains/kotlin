// RESOLVE_FILE

@file:[Deprecated("deprecated file") Anno(1)]
package one

annotation class Anno(val i: Int)

val a = 1
val b = 2 + a
val c = b + a

fun test1() {}
@Deprecated("")
@Anno(2)
fun test2() {}

class A {
    fun test3() {}
    @Deprecated("")
    @Anno(3)
    fun test4() {}
}

@Anno(4)
@Deprecated("")
class B
