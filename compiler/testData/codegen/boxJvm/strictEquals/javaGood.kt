// LANGUAGE: +StrictEquals
// WITH_STDLIB

// FILE: p/JavaGoodExplicit.java
package p;

public class JavaGoodExplicit extends KotlinClass {
    public JavaGoodExplicit(int x) {
        super(x);
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof JavaGoodExplicit && getX() == ((KotlinClass) other).getX();
    }
}

// FILE: p/JavaGoodImplicit.java
package p;

public class JavaGoodImplicit extends KotlinClass {
    public JavaGoodImplicit(int x) {
        super(x);
    }
}

// FILE: p/main.kt
package p

open class KotlinClass(val x: Int) {
    override fun equals(@EqualityBound(KotlinClass::class) other: Any?): Boolean = other !is JavaGoodExplicit && x == other.x
}

fun box(): String {
    val e42 = JavaGoodExplicit(42)
    val e42_2: Any? = JavaGoodExplicit(42)
    val i42 = JavaGoodImplicit(42)
    val i42_2: Any? = JavaGoodImplicit(42)
    val k42_2: Any? = KotlinClass(42)
    val any: Any? = Any()

    if (e42 != e42_2 || e42_2.x != 42) return "Fail#1"
    if (i42 != i42_2 || i42_2.x != 42) return "Fail#2"
    if (i42 != k42_2 || k42_2.x != 42) return "Fail#3"
    if (e42 == k42_2) return "Fail#4"
    if (e42 == any) return "Fail#5"
    if (i42 == any) return "Fail#6"
    return "OK"
}
