// SCOPE_DUMP: D:x;getX;y;getY

// FILE: B.java
public abstract class B extends A {
    @Override
    public int getX() { return 0; }
}

// FILE: C.java
public interface C {
    int getX();
    int getY();
}

// FILE: D.java
public abstract class D extends B implements C {}

// FILE: main.kt

abstract class A {
    open val x: Int
        get() = 1
    open val y: Int
        get() = 2
}

class DImpl : D()

