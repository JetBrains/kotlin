// FILE: SamInterface.java

public interface SamInterface {
    public boolean foo(String arg);
}

// FILE: test.kt

class Wrapper(val si: SamInterface?)

fun test(w: Wrapper?) {
    Wrapper(w?.let { it.si })
}
