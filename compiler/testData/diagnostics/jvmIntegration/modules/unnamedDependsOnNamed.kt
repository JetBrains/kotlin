// JDK_KIND: FULL_JDK_11
// FORCE_COMPILE_AS_JAVA_MODULE
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports foo;
}

// FILE: foo/Foo.java
package foo;

public class Foo {}

// FILE: foo/impl/Impl.java
package foo.impl;

public class Impl {}

// MODULE: moduleB1(moduleA)
// KOTLINC_ARGS: -Xadd-modules=moduleA
// FILE: usage.kt
import foo.Foo
import foo.impl.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>Impl<!>

fun usage() {
    Foo()

    <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>Impl<!>()
}

// FILE: Usage.java
public class Usage {
    public static void main(String[] args) {
        new foo.Foo();
    }
}

// MODULE: moduleB2(moduleA)
// Also check that -Xadd-modules=ALL-MODULE-PATH has the same effect as -Xadd-module=moduleA, i.e. adds moduleA to the roots
// KOTLINC_ARGS: -Xadd-modules=moduleA
// FILE: usage.kt
import foo.Foo
import foo.impl.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>Impl<!>

fun usage() {
    Foo()

    <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>Impl<!>()
}

// FILE: Usage.java
public class Usage {
    public static void main(String[] args) {
        new foo.Foo();
    }
}
