// TARGET_BACKEND: JVM
// FILE: JI.java
public interface JI {
    @Override
    public abstract boolean equals(Object o);
    @Override
    public abstract int hashCode();
    @Override
    public abstract String toString();
}

// FILE: JC.java
public abstract class JC {
    @Override
    public abstract boolean equals(Object o);
    @Override
    public abstract int hashCode();
    @Override
    public abstract String toString();
}

// FILE: JC2.java
public abstract class JC2 extends JC {
}

// FILE: box.kt

interface KI : JI
class X : KI {
    override fun equals(other: Any?): Boolean = true
    override fun hashCode(): Int = 0
    override fun toString(): String = ""
}

abstract class KC : JC()
class Y : KC() {
    override fun equals(other: Any?): Boolean = true
    override fun hashCode(): Int = 0
    override fun toString(): String = ""
}

abstract class KC2 : JC2()
class Z : KC2() {
    override fun equals(other: Any?): Boolean = true
    override fun hashCode(): Int = 0
    override fun toString(): String = ""
}

fun box(): String {
    X().equals(X())
    X().hashCode()
    X().toString()

    Y().equals(Y())
    Y().hashCode()
    Y().toString()

    Z().equals(Z())
    Z().hashCode()
    Z().toString()

    return "OK"
}
