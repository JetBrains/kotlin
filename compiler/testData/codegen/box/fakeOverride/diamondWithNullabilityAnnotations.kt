// TARGET_BACKEND: JVM_IR
// ISSUE: KT-65592

// FILE: A.java
public interface A {
    String f();
}

// FILE: AImpl.java
import org.jetbrains.annotations.NotNull;

public class AImpl implements A {
    @Override
    @NotNull
    public String f() {
        return "OK";
    }
}

// FILE: B.kt
interface B : A

// FILE: BImpl.kt
open class BImpl : AImpl(), B

// FILE: C.java
public interface C extends B { }

// FILE: CImpl.java
public class CImpl extends BImpl implements C { }

// FILE: D.java
public interface D extends C { }

// FILE: DImpl.java
public class DImpl extends CImpl implements D { }

// FILE: box.kt
class Z : DImpl(), D

fun box(): String {
    return Z().f()
}
