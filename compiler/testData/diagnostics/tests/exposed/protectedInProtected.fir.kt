// See KT-9540 

// all protected should have lower bound that is more permissive than private
// protected and internal should have lower bound that is more permissive than private
open class A {
    private interface B
    protected open class C {
        protected interface D : B
        internal interface E : B, D
    }
}

