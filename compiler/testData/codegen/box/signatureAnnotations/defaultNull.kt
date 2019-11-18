// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

public class A {
    public Integer foo(@DefaultNull Integer x) { return x; }
    public Integer bar(@DefaultNull Integer x) { return x; }

    public Integer baz(@DefaultValue("42") Integer x) { return x; }
}

// FILE: AInt.java
import kotlin.annotations.jvm.internal.*;

public interface AInt {
    public Integer foo(@DefaultValue("42") Integer x);
    public Integer bar(@DefaultNull Integer x);
}

// FILE: B.java

public class B extends A {
    public Integer foo(Integer x) { return x; }
}

// FILE: C.java
import kotlin.annotations.jvm.internal.*;

public class C extends A {
    public Integer foo(@DefaultValue("42") Integer x) { return x; }

    public Integer baz(@DefaultNull Integer x) { return x; }
}

// FILE: D.java

public class D extends A implements AInt {
}

// FILE: test.kt
fun box(): String {
    if (A().foo() != null) return "FAIL 0"

    if (B().foo() != null) return "FAIL 1"
    if (B().bar() != null) return "FAIL 2"

    if (C().foo() != null) return "FAIL 3"
    if (C().baz() != 42) return "FAIL 4"

    if (D().baz() != 42) return "FAIL 5"

    return "OK"
}