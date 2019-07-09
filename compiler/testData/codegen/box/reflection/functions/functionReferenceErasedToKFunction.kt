// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

import kotlin.jvm.functions.Function2;
import kotlin.reflect.KFunction;

public class J {
    public static String go() {
        KFunction<String> fun = K.Companion.getRef();
        Object result = ((Function2) fun).invoke(new K(), "KO");
        return (String) result;
    }
}

// FILE: K.kt

class K {
    fun reverse(s: String): String {
        return s.reversed()
    }

    companion object {
        fun getRef() = K::reverse
    }
}

fun box(): String {
    return J.go()
}
