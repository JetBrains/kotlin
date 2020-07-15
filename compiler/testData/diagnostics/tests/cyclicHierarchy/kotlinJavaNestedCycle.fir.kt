// FILE: ExceptionTracker.kt

interface ExceptionTracker : <!EXPOSED_SUPER_INTERFACE!>LockBasedStorageManager.ExceptionHandlingStrategy<!> {
}

// FILE: StorageManager.kt

interface StorageManager : ExceptionTracker {
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
