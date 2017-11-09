// FILE: Derived.kt
// IR_FILE: jvmInstanceFieldReference.txt
class Derived: Base() {
    init {
        value = 0
    }

    fun getValue() = value

    fun setValue(value: Int) {
        this.value = value
    }
}

// FILE: Base.java
public class Base {
    public int value;
}

