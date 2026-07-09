// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JavaBase.java
public class JavaBase {
    public String flag = "base";
}

// FILE: test.kt
open class KotlinSub : JavaBase()

class KotlinSubSub : KotlinSub() {
    fun foo(): String {
        return flag
    }
}

fun box(): String {
    return KotlinSubSub().foo()
}
