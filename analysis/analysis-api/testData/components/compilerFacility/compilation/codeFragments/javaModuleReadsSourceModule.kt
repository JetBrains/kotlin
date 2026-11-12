// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports a;
}

// FILE: a/A.java
package a;

public class A {}

// MODULE: context(moduleA)
// COMPILATION_ERRORS
// ^The LibrarySource mode does not support module dependencies.
// FILE: module-info.java
module moduleB {
    requires kotlin.stdlib;
    requires moduleA;
}

// FILE: context.kt
fun context() {
    <caret_context>Unit
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
a.A()
