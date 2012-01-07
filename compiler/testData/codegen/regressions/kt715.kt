import std.*

val test = "lala".javaClass

val test2 = typeinfo<Int>().javaClassForType

fun box(): String {
    if(test.getCanonicalName() != "java.lang.String") return "fail"
    System.out?.println(test2.getCanonicalName())
    if(test2.getCanonicalName() != "java.lang.Integer") return "fail"
    return "OK"
}