// RUN_PIPELINE_TILL: FRONTEND

// JLS 6.6.2 accessibility of a `protected` nested class across packages, where the use site is a
// subclass of the declaring class only *through* a Kotlin source class: `b.J extends a.K extends
// a.JBase`. The reference `JMid.Nested` is qualified, so the nested class itself is found through
// the all-Java chain `a.JMid -> a.JBase`; only the accessibility check walks the use site's own
// hierarchy and therefore needs the direct supertypes of the Kotlin `a.K`.
//
// As in `inheritedNestedClassThroughKotlinSupertype.kt`, the reference stands in an `extends` clause
// forced by `KotlinSub : J.Inner()` from the first Kotlin file, so it is resolved before `a.K`
// reaches SUPER_TYPES. Answering "K has no supertypes" there makes an accessible protected class
// look inaccessible.

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
