// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80719

open class Owner {
    open class Base {
        protected open fun foo() {}
    }

    protected class Derived : Base() {
        <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override fun foo() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, override */
