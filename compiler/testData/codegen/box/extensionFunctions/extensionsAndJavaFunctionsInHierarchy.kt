// TARGET_BACKEND: JVM
// FILE: BaseJava.java
public class BaseJava {
    public String test(Integer a){
        return "BaseJava test ";
    }

    public String getA(Integer a){
        return "BaseJava getA ";
    }
}

// FILE: test.kt
var result = ""

class KotlinBaseImpl : BaseJava() {
    val Int.a: String
        get() = "KotlinBaseImpl a "

    fun Int.test(): String {
        return "KotlinBaseImpl test"
    }
}

fun box(): String {
    val b = KotlinBaseImpl()
    with(b) {
        result += getA(1)
        result += 1.a
        result += test(1)
        result += 1.test()
        return if (result == "BaseJava getA KotlinBaseImpl a BaseJava test KotlinBaseImpl test") "OK"
        else "fail"
    }
}