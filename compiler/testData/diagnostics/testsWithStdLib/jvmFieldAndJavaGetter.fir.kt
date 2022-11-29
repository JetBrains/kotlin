// MODULE: lib
// FILE: TestA.java

public abstract class TestA {
    public abstract int getProp();
}

// MODULE: main(lib)
// FILE: TestB.kt

abstract class TestB : TestA() {
    @JvmField
    protected var prop: Int = 0
}
