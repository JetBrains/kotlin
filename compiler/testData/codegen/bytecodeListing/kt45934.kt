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
