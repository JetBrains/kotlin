// QUERY: annotations
// WITH_STDLIB
package pack

@MyAnno(0) @JvmName("myName")
fun fo<caret>o(i: Int): String = "s"

annotation class MyAnno(val count: Int)