// TARGET_BACKEND: JVM
// FILE: JavaImpl.java
public class JavaImpl implements Interface {
    @Override
    public String accept(int a, String i){
        return "K";
    }
}

// FILE: test.kt
var result = ""

interface Interface {
    fun Int.accept(i: String): String {
        return "O"
    }
}

class KotlinImpl : Interface

fun box(): String {
    with(KotlinImpl()){
        result += 1.accept("1")
    }
    with(JavaImpl()){
        result+= 1.accept("1")
    }
    return result
}