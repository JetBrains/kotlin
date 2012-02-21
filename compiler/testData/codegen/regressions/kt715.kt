import std.*

val test = "lala".javaClass

val test2 = javaClass<Iterator<Int>> ()

fun box(): String {
    if(test.getCanonicalName() != "java.lang.String") return "fail"
    if(test2.getCanonicalName() != "jet.Iterator") return "fail"
    return "OK"
}