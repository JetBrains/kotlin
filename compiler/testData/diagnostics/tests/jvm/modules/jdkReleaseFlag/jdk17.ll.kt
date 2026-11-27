// LL_FIR_DIVERGENCE
// The LL FIR path resolves JDK symbols before it checks Java module readability, so it reports
// `JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE`. The compiler path filters unread JDK modules during symbol resolution, so it reports
// `UNRESOLVED_REFERENCE`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// JDK_KIND: FULL_JDK_17
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

// MODULE: module17
// JDK_RELEASE: 17
// FILE: module-info.java
module module17 {
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
    val z: <!JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE!>javax.swing.JFrame?<!> = null
}

// FILE: foo/Foo2.kt
package foo;

public class Foo2 {}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration */
