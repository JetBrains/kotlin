// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

import java.util.*;

public class J {
    public static String nullValue() {
        return null;
    }
}

// FILE: test.kt

class MySet : Set<String> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun contains(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterator(): Iterator<String> {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }
}

fun box(): String {
    val mySet = MySet()

    // no UnsupportedOperationException thrown
    mySet.contains(J.nullValue())
    J.nullValue() in mySet

    val set: Set<String> = mySet
    set.contains(J.nullValue())
    J.nullValue() in set

    val anySet: Set<Any?> = mySet as Set<Any?>
    anySet.contains(J.nullValue())
    anySet.contains(null)
    J.nullValue() in anySet
    null in anySet

    return "OK"
}
