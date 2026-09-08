// FILE: lib/JavaBase.java
package lib

public class JavaBase {
    public void foo() {}
}

// FILE: lib/JavaImpl.java
package lib

public class JavaImpl extends JavaBase {
    @Override
    public void foo() {}
}

// FILE: main.kt
import lib.*

fun test(impl: JavaImpl) {}
