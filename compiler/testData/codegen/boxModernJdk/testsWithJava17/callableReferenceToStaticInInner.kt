// LANGUAGE: +CompanionBlocksAndExtensions
// TARGET_BACKEND: JVM

// FILE: test/JavaClass.java
package test;

public class JavaClass<T> {
    public class Inner {
        public static class Static { }
        public static void staticFun() { }
        public class SubInner { }
    }
}

// FILE: test/main.kt
package test

class KotlinClass<T> {
    inner class Inner {
        companion {
            fun static() { }
        }
    }
}

companion fun KotlinClass.Inner.extension() { }

fun box(): String {
    JavaClass.Inner::Static
    JavaClass.Inner::staticFun
    JavaClass<*>.Inner::SubInner
    KotlinClass.Inner::static
    KotlinClass.Inner::extension
    return "OK"
}
