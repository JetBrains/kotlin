// TARGET_BACKEND: JVM
// FILE: Simple.java

public interface Simple {
    default String test(String s) {
        return s + "Fail";
    }
}

// FILE: main.kt

interface KInterface: Simple {
    override fun test(s: String): String {
        return s + "K"
    }
}

class Test : KInterface {

}

fun box(): String {
    return Test().test("O")
}
