// See KT-9540 

// all protected should have lower bound that is more permissive than private
open class A {
    private interface B
    protected open class C {
        protected interface D : <!EXPOSED_SUPER_INTERFACE!>B<!>
    }
}

// protected and internal should have lower bound that is more permissive than private
open class AA {
    private interface BB
    protected open class CC {
        internal interface DD : <!EXPOSED_SUPER_INTERFACE!>BB<!>
    }
}
