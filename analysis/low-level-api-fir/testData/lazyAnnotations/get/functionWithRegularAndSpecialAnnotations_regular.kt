// QUERY: get: pack/MyAnno
// WITH_STDLIB
package pack

@MyAnno(0) @JvmName("myName")
fun f<caret>oo(i: Int): String = "s"

annotation class MyAnno(val count: Int)