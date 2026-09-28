// RUN_PIPELINE_TILL: FRONTEND

// FILE: a/JBase.java
package a;

public class JBase {
    public static class Nested {
        public int fromBase() { return 1; }
    }
}

// FILE: a/J.java
package a;

public class J extends K {
    public static class Inner extends Nested {
    }
}

// FILE: main.kt
package a

class KotlinSub : J.Inner()

fun test(inner: J.Inner) = inner.fromBase()

// FILE: kotlinLink.kt
package a

open class K : JBase()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType */
