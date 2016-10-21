open class A(val a:Int, val b:Int)

open class B(val c:Int, d:Int):A(c, d)

//class C():B(42)

fun foo(i:Int, j:Int):Int {
   val b = B(i, j)
   return b.c
}