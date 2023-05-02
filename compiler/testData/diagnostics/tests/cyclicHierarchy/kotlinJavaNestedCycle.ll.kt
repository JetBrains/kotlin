// LL_FIR_DIVERGENCE
// The compiler doesn't guarantee exhaustiveness in reporting of inheritance cycles, so the compiler and LL FIR results are equally valid.
// LL_FIR_DIVERGENCE
// FILE: ExceptionTracker.kt

interface ExceptionTracker : <!CYCLIC_INHERITANCE_HIERARCHY!>LockBasedStorageManager.ExceptionHandlingStrategy<!> {
}

// FILE: StorageManager.kt

interface StorageManager : <!CYCLIC_INHERITANCE_HIERARCHY!>ExceptionTracker<!> {
    fun foo()
}

// FILE: LockBasedStorageManager.java

class LockBasedStorageManager extends StorageManager {
    interface ExceptionHandlingStrategy {
        void bar();
    }

    @Override
    void foo() {}
}
