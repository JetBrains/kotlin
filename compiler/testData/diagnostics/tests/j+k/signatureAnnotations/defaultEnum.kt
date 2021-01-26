// ANDROID_ANNOTATIONS
// FILE: Signs.java

public enum Signs {
    HELLO,
    WORLD;

    public static final Signs X;
    public static final class NOT_ENTRY_EITHER {}
}

// FILE: Mixed.java
public enum Mixed {
    NOT_ENTRY_EITHER;

    public static final class NOT_ENTRY_EITHER {}
}

// FILE: B.kt
enum class B {
    X,
    Y;
}

// FILE: A.java
import kotlin.annotations.jvm.internal.*;

class A {
    public Signs a(@DefaultValue("HELLO") Signs arg)  {
        return arg;
    }

    public B b(@DefaultValue("Y") B arg) {
        return arg;
    }

    public void foooo(@DefaultValue("ok") B arg) {
    }

    public Signs bar(@DefaultValue("X") Signs arg)  {
        return arg;
    }

    public Signs baz(@DefaultValue("NOT_ENTRY_EITHER") Signs arg) {
        return arg;
    }

    public Mixed bam(@DefaultValue("NOT_ENTRY_EITHER") Mixed arg) {
        return arg;
    }

}

// FILE: test.kt
fun test(){
    val a = A()
    a.a()
    a.a(Signs.HELLO)
    a.b()
    a.b(B.X)
    a.foooo(<!NO_VALUE_FOR_PARAMETER!>)<!>
    a.foooo(B.Y)
    a.bar(<!NO_VALUE_FOR_PARAMETER!>)<!>
    a.baz(<!NO_VALUE_FOR_PARAMETER!>)<!>

    a.bam(<!NO_VALUE_FOR_PARAMETER!>)<!>
}
