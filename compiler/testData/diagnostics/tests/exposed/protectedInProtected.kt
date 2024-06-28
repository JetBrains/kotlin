// FIR_IDENTICAL
// See KT-9540 

// all protected should have lower bound that is more permissive than private
// protected and internal should have lower bound that is more permissive than private
open class A {
    private interface B
    protected open class C {
        protected interface D : <!EXPOSED_SUPER_INTERFACE!>B<!>
        internal interface E : <!EXPOSED_SUPER_INTERFACE!>B<!>, <!EXPOSED_SUPER_INTERFACE!>D<!>
    }
}

open class Owner {
    protected open class A : Owner() {
        protected open class ProtectedInA
    }

    protected abstract class B : A() {
        protected abstract fun foo(): ProtectedInA
    }
}
