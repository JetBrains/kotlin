import std.*

val test = "lala".javaClass

fun box(): String {
    if(test.getCanonicalName() != "java.lang.String") return "fail"
    return "OK"
}