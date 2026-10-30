// LL_FIR_DIVERGENCE
// The LL test platform doesn't build the Java module graph from source `module-info.java` files: all default JDK
// modules stay visible, so `javax.swing` from `java.desktop`, which is not required, is resolved.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// JDK_KIND: FULL_JDK_11
// MODULE: module
// FILE: module-info.java
module module {
    exports foo;
    requires kotlin.stdlib;
}

// FILE: foo/Foo.kt
package foo;

public class Foo {
    val z: java.nio.ByteBuffer? = null
}

// MODULE: module9
// JDK_RELEASE: 9
// FILE: module-info.java
module module9 {
    exports foo;
    requires kotlin.stdlib;
}

// FILE: foo/Foo.kt
package foo;

public class Foo {
    val z: java.nio.ByteBuffer? = null
}

// MODULE: module11
// JDK_RELEASE: 11
// FILE: module-info.java
module module11 {
    exports foo;
    requires kotlin.stdlib;
}

// FILE: foo/Foo.kt
package foo;

public class Foo {
    val z: java.nio.ByteBuffer? = null
}

// MODULE: moduleSwing
// JDK_RELEASE: 9
// FILE: module-info.java
module moduleSwing {
    exports foo;
    requires kotlin.stdlib;
}

// FILE: foo/Foo.kt
package foo;

public class Foo {
    //no requirement
    val z: javax.swing.JFrame? = null
}

// FILE: foo/Foo2.kt
package foo;

public class Foo2 {}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration */
