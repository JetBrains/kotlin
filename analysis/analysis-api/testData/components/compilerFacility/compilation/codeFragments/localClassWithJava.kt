// MODULE: context

// FILE: lib/Base.java
package lib;

public abstract class Base {
    public void boo() {}
}

// FILE: context.kt
package lib

class Derived: Base() {
    fun bar() {
        boo()
        class LocalClass {
            fun foo() = 42
        }

        <caret_context>"".toString()
    }
}

fun main() {
    Derived().bar()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
LocalClass().foo()