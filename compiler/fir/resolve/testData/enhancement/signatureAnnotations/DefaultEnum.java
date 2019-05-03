// FILE: Signs.java
// ANDROID_ANNOTATIONS

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

// FILE: A.java
import kotlin.annotations.jvm.internal.*;

class A {
    public Signs a(@DefaultValue("HELLO") Signs arg)  {
        return arg;
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
