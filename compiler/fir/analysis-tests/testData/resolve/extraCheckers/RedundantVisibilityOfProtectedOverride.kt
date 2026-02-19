// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80719

open class Owner {
    open class Base {
        protected open fun foo() {}

        internal open fun bar() {}
    }

    protected class ProtectedDerived : Base() {
        // public is NOT redundant here, as it allows foo to be used in scope "Owner & its inheritors",
        // but without modifier at all foo is usable in scope "Base & its inheritors" which is narrower
        public override fun foo() {}

        // public is NOT redundant here, as it allows foo to be used in scope "Owner & its inheritors, including other module inheritors",
        // but without modifier at all foo is usable only in module scope which is narrower
        public override fun bar() {}
    }

    internal class InternalDerived : Base() {
        // public is NOT redundant here, as it allows foo to be used in module scope,
        // but without modifier at all foo is usable in scope "Base & its inheritors" which is narrower
        public override fun foo() {}

        // public is redundant, bar is still usable only in module scope
        <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override fun bar() {}
    }

    private class PrivateDerived : Base() {
        // public is redundant, foo is still usable only in scope "Base & its inheritors"
        // However we don't report it because of KT-82487 corner case
        public override fun foo() {}

        // public is redundant, bar is still usable only in module scope
        <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override fun bar() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, override */
