// IGNORE_BACKEND_K2: JVM_IR
// FIR status: delegated implementation for `foo` is generated in `C`, which makes this code execute fine at runtime.
//             For K1, invoking `C.foo` leads to AbstractMethodError, see KT-46120.
//             Probably there should be a frontend error. (But if not, then K2 is correct.)

// FILE: kt45934.kt

class C(client: J) : I by client

// FILE: I.java

import java.util.List;

public interface I {
    <C> List<C> foo();
}

// FILE: J.java

import java.util.List;

public class J implements I {
    @Override
    public List<String> foo() {
        return null;
    }
}
