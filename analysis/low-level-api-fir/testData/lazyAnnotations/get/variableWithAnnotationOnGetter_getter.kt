// QUERY: get: pack/MyAnno
// WITH_STDLIB
package pack

@get:MyAnno("str")
var variable: Int = 0
    ge<caret>t() = 1

annotation class MyAnno(val s: String)