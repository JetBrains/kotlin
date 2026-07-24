// RUN_PIPELINE_TILL: BACKEND
// SCOPE_DUMP: p.ThroughMissedJavaIF:equals, p.ThroughExplicitJavaIF:equals
// SCOPE_DUMP: p.MissedIntersectJavaAC:equals, p.ExplicitIntersectJavaAC:equals
// SCOPE_DUMP: p.MissedIntersectJavaAC_2:equals, p.ExplicitIntersectJavaAC_2:equals
// SCOPE_DUMP: p.MissedIntersectJavaAC_3:equals, p.ExplicitIntersectJavaAC_3:equals
// SCOPE_DUMP: p.MissedJavaIF:equals, p.ExplicitJavaIF:equals
// SCOPE_DUMP: p.MissedPostSkipper:equals, p.ExplicitPostSkipper:equals

// FILE: p/Originals.kt
package p

interface KotlinIF { // KotlinIF
    fun kotlinIF() = Unit
    override fun equals(@EqualityBound(KotlinIF::class) other: Any?): Boolean
}

abstract class KotlinAC { // KotlinAC
    fun kotlinAC() = Unit
    abstract override fun equals(@EqualityBound(KotlinAC::class) other: Any?): Boolean
}

// FILE: p/MissedJavaIF.java
package p;

public interface MissedJavaIF extends KotlinIF { // KotlinIF
}

// FILE: p/ExplicitJavaIF.java
package p;

public interface ExplicitJavaIF extends KotlinIF { // KotlinIF
    public boolean equals(Object other);
}

// FILE: p/ExplicitJavaIF_2.java
package p;

public interface ExplicitJavaIF_2 extends ExplicitJavaIF { // KotlinIF
    public boolean equals(Object other);
}

// FILE: p/MissedJavaAC.java
package p;

public abstract class MissedJavaAC extends KotlinAC {
}

// FILE: p/ExplicitJavaAC.java
package p;

public abstract class ExplicitJavaAC extends KotlinAC { // KotlinAC
    public abstract boolean equals(Object other);
}

// FILE: p/MissedIntersectJavaAC.java
package p;

public abstract class MissedIntersectJavaAC extends KotlinAC implements KotlinIF { // KotlinAC & KotlinIF
}

// FILE: p/ExplicitIntersectJavaAC.java
package p;

public abstract class ExplicitIntersectJavaAC extends KotlinAC implements KotlinIF { // KotlinAC & KotlinIF
    public abstract boolean equals(Object other);
}

// FILE: p/MissedIntersectJavaAC_2.java
package p;

public abstract class MissedIntersectJavaAC_2 extends ExplicitJavaAC implements ExplicitJavaIF_2 { // KotlinAC & KotlinIF
}

// FILE: p/ExplicitIntersectJavaAC_2.java
package p;

public abstract class ExplicitIntersectJavaAC_2 extends ExplicitJavaAC implements ExplicitJavaIF_2 { // KotlinAC & KotlinIF
    public abstract boolean equals(Object other);
}

// FILE: p/MissedIntersectJavaAC_3.java
package p;

public abstract class MissedIntersectJavaAC_3 extends MissedJavaAC implements MissedJavaIF {
}

// FILE: p/ExplicitIntersectJavaAC_3.java
package p;

public abstract class ExplicitIntersectJavaAC_3 extends MissedJavaAC implements MissedJavaIF {
    public abstract boolean equals(Object other);
}

// FILE: p/ExplicitPostSkipper.java
package p;

public abstract class ExplicitPostSkipper extends Skipper {
    public abstract boolean equals(Object other);
}

// FILE: p/MissedPostSkipper.java
package p;

public abstract class MissedPostSkipper extends Skipper {
}

// FILE: p/Inheritors.kt
package p

abstract class Skipper : ExplicitJavaAC()

class ThroughMissedJavaIF : MissedJavaIF { // KotlinIF
    override fun equals(other: Any?): Boolean {
        other.kotlinIF()
        return true
    }
}

class ThroughExplicitJavaIF : ExplicitJavaIF { // KotlinIF
    override fun equals(other: Any?): Boolean {
        other.kotlinIF()
        return true
    }
}

class ThroughExplicitJavaIF_2 : ExplicitJavaIF_2 { // KotlinIF
    override fun equals(other: Any?): Boolean {
        other.kotlinIF()
        return true
    }
}

class ThroughExplicitJavaAC : ExplicitJavaAC() {
    override fun equals(other: Any?): Boolean {
        other.kotlinAC()
        return true
    }
}

class ThroughMissedJavaAC : MissedJavaAC() {
    override fun equals(other: Any?): Boolean {
        other.kotlinAC()
        return true
    }
}

fun useSite_1(
    any: Any?,
    p1: MissedIntersectJavaAC,
    p2: ExplicitIntersectJavaAC,
    p3: MissedIntersectJavaAC_2,
    p4: ExplicitIntersectJavaAC_2,
    p5: MissedIntersectJavaAC_3,
    p6: ExplicitIntersectJavaAC_3,
    p7: MissedPostSkipper,
    p8: ExplicitPostSkipper,
) {
    if (p1 == any) {
        any.kotlinIF()
        any.kotlinAC()
    }
    if (p2 == any) {
        any.kotlinIF()
        any.kotlinAC()
    }
    if (p3 == any) {
        any.kotlinIF()
        any.kotlinAC()
    }
    if (p4 == any) {
        any.kotlinIF()
        any.kotlinAC()
    }
    if (p5 == any) {
        any.kotlinIF()
        any.kotlinAC()
    }
    if (p6 == any) {
        any.kotlinIF()
        any.kotlinAC()
    }
    if (p7 == any) {
        any.kotlinAC()
    }
    if (p8 == any) {
        any.kotlinAC()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, operator, override, smartcast */
