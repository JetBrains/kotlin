// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: JFun.java

class JFun implements kotlin.jvm.functions.Function0<String> {
    public String invoke() {
        return "OK";
    }
}

// FILE: test.kt

fun box(): String {
    val jfun = JFun()
    val jf = jfun as Any
    if (jf is Function0<*>) return jfun()
    else return "Failed: jf is Function0<*>"
}
