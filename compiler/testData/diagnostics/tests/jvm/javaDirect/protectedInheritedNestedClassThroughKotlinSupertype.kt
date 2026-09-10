// RUN_PIPELINE_TILL: FRONTEND

// FILE: a/JBase.java
package a;

public class JBase {
    protected static class Nested {
        public int fromBase() { return 1; }
    }
}

// FILE: a/JMid.java
package a;

public class JMid extends JBase {
}

// FILE: b/J.java
package b;

import a.JMid;
import a.K;

public class J extends K {
    public static class Inner extends JMid.Nested {
    }
}

// FILE: main.kt
package b

class KotlinSub : J.Inner()

fun test(inner: J.Inner) = inner.fromBase()

// FILE: kotlinLink.kt
package a

open class K : JBase()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType */
