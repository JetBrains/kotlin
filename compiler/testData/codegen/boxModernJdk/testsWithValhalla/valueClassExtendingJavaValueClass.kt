// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses

// MODULE: lib
// FILE: JavaAbstractValBinary.java
public abstract value class JavaAbstractValBinary {
    protected final int base;

    protected JavaAbstractValBinary(int base) {
        this.base = base;
    }

    public abstract int get();

    public int sum() {
        return base + get();
    }
}

// MODULE: main(lib)
// FILE: JavaAbstractValSource.java
public abstract value class JavaAbstractValSource {
    public abstract int get();

    public int twice() {
        return get() * 2;
    }
}

// FILE: test.kt
value class BinaryChild(val x: Int) : JavaAbstractValBinary(1) {
    override fun get(): Int = x
}

value class SourceChild(val x: Int) : JavaAbstractValSource() {
    override fun get(): Int = x
}

fun box(): String {
    val binary = BinaryChild(41)
    if (!binary.javaClass.isValue) return "BinaryChild is not a value class"
    if (binary.sum() != 42) return "BinaryChild.sum: ${binary.sum()}"
    if (binary != BinaryChild(41)) return "BinaryChild.equals"
    val source = SourceChild(21)
    if (!source.javaClass.isValue) return "SourceChild is not a value class"
    if (source.twice() != 42) return "SourceChild.twice: ${source.twice()}"
    return "OK"
}
