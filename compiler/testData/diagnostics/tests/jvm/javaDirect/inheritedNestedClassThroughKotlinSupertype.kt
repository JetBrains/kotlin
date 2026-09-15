// RUN_PIPELINE_TILL: FRONTEND

// JLS 6.5.2: `Nested` is inherited by `J` from `JBase`, but the inheritance path goes *through* a
// Kotlin source class, so resolving the simple name requires the direct supertypes of `K`.
//
// The reference stands in the `extends` clause of `J.Inner`, and `KotlinSub : J.Inner()` in the
// first Kotlin file forces that clause to be resolved while the SUPER_TYPES phase is running — at
// which point `K`, declared in a later file, has not reached that phase itself. Answering "K has no
// supertypes" there turns correct code into an unresolved reference.

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
