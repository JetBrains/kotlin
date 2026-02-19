// TARGET_BACKEND: JVM
// FILE: JavaImpl.java
public interface JavaImpl extends Interface {
    @Override
    default String accept(int a, String i) {
        return "OK";
    }
}

// FILE: test.kt
interface Interface {
    fun Int.accept(i: String): String
}

class KotlinImpl : JavaImpl

fun box(): String {
    with(KotlinImpl()){
        return 1.accept("1")
    }
}