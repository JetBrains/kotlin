import kotlin.*

val test = "lala".javaClass

val test2 = javaClass<Iterator<Int>> ()

fun box(): String {
    if(test.getCanonicalName() != "java.lang.String") return "fail"
    if(test2.getCanonicalName() != "java.util.Iterator") return "fail"
    return "OK"
}
