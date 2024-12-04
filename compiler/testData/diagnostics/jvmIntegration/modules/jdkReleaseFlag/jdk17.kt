// FIR_IDENTICAL
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
// KOTLINC_ARGS: -Xjdk-release=9
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
// KOTLINC_ARGS: -Xjdk-release=11
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
// KOTLINC_ARGS: -Xjdk-release=17
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
// KOTLINC_ARGS: -Xjdk-release=9
// FILE: module-info.java
module moduleSwing {
    exports foo;
    requires kotlin.stdlib;
}

// FILE: foo/Foo.kt
package foo;

public class Foo {
    //no requirement
    val z: javax.<!UNRESOLVED_REFERENCE!>swing<!>.JFrame? = null
}

// FILE: foo/Foo2.kt
package foo;

public class Foo2 {}
