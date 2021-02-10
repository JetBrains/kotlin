// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: test/E.java

package test;

import java.util.Set;
import java.util.EnumSet;

public enum E {
    INSTANCE;
    
    public static int foo = 42;
    
    public static final Set<E> INSTANCES = EnumSet.of(INSTANCE);
}

// MODULE: main(lib)
// FILE: 1.kt

import test.E

fun box(): String {
    val instances = E.INSTANCES
    if (E.foo != 42)
        return "Wrong foo ${E.foo}"
    if (instances.size != 1)
        return "Wrong size ${instances.size}"
    if (E.INSTANCES.iterator().next() != E.INSTANCE)
        return "Wrong instance ${E.INSTANCES.iterator().next()}"
    return "OK"
}
