// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80719

open class Owner {
    open class Base {
        protected open fun foo() {}
    }

    protected class Derived : Base() {
        // public is NOT redundant here, as it allows foo to be used in scope "Owner & its inheritors",
        // but without modifier at all foo is usable in scope "Base & its inheritors" which is narrower
        public override fun foo() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, override */
