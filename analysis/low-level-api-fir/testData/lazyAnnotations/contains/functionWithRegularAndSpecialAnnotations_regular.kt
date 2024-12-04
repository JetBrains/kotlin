// QUERY: contains: pack/MyAnno
// WITH_STDLIB
package pack

@MyAnno(0) @JvmName("myName")
fun foo<caret>(i: Int): String = "s"

annotation class MyAnno(val count: Int)