// FILE: VarHandle.java
public class VarHandle {
    public void set(Object... args) {}
}

// FILE: main.kt
class Some {
    fun test(handle: VarHandle) {
        handle.set(this, false)
    }
}
