// FILE: A.java

public abstract class A {
    abstract public char charAt(int i);
}

// FILE: B.java

abstract public class B extends A implements CharSequence {
    public char charAt(int i) { return '1'; }
}

// FILE: main.kt

abstract class C1 : B()

abstract class C2 : B() {
    override fun get(index: Int) = '1'
}
