// RUN_PIPELINE_TILL: FRONTEND
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
// ADDITIONAL_JAVA_MODULES: moduleA
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
