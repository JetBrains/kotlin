// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// WITH_STDLIB

// FILE: p/JavaBad.java
package p;

public class JavaBad extends KotlinClass {
    public JavaBad(int x) {
        super(x);
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof KotlinSuper;
    }
}


// FILE: p/main.kt
package p

abstract class KotlinSuper {
    override fun equals(@EqualityBound(KotlinClass::class) other: Any?): Boolean = true
}

abstract class KotlinClass(val x: Int) : KotlinSuper() {
    override fun equals(@EqualityBound(KotlinClass::class) other: Any?): Boolean = x == other.x
}

fun box(): String {
    try {
        val other: KotlinSuper = object : KotlinSuper() {}
        if (JavaBad(42) == other) {
            other.x
        }
        return "FAIL"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
