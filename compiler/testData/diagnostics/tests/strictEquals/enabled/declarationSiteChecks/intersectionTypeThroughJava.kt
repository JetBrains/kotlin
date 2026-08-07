// RUN_PIPELINE_TILL: FRONTEND
// SCOPE_DUMP: p.S:equals, p.T:equals

// FILE: p/KotlinBase.kt
package p

interface KotlinSuperIF {
    override fun equals(other: Any?): Boolean
}

interface KotlinIF : KotlinSuperIF {
    override fun equals(@EqualityBound(KotlinIF::class) other: Any?): Boolean
}

abstract class KotlinAC {
    override fun equals(@EqualityBound(KotlinAC::class) other: Any?): Boolean = true
}

open class KotlinClassWithSuperEB : KotlinSuperIF {
    override fun equals(@EqualityBound(KotlinSuperIF::class) other: Any?): Boolean = true
}

// FILE: p/JavaAmbiguous.java
package p;

public abstract class JavaAmbiguous extends KotlinAC implements KotlinIF {
}

// FILE: p/JavaGeneric.java
package p;

public abstract class JavaGeneric<T> extends KotlinAC implements KotlinIF {
}

// FILE: p/JavaWithWrongInheritedImpl.java
package p;

// This is quite cursed: from our POV, this class has KotlinIF equality bound.
// However, it uses the implementation of KotlinClassWithSuperEB (which has KotlinSuperIF equalityBound).
// Java, obviously, does not report this.
// Should we do something about it, at least in the kotlin inheritors?
public class JavaWithWrongInheritedImpl extends KotlinClassWithSuperEB implements KotlinIF {
}

// FILE: p/JavaWithWrongInheritedImplGeneric.java
package p;

public class JavaWithWrongInheritedImplGeneric<K> extends KotlinClassWithSuperEB implements KotlinIF {
}

// FILE: test.kt
package p

<!INHERITED_INTERSECTION_EQUALITY_BOUND!>class A : JavaAmbiguous()<!>
<!INHERITED_INTERSECTION_EQUALITY_BOUND!>class B : JavaGeneric<CharSequence>()<!>

class C : JavaAmbiguous() {
    <!INHERITED_INTERSECTION_EQUALITY_BOUND!>override fun equals(other: Any?): Boolean = true<!>
}

class D : JavaGeneric<CharSequence>() {
    <!INHERITED_INTERSECTION_EQUALITY_BOUND!>override fun equals(other: Any?): Boolean = true<!>
}

<!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>class S : JavaWithWrongInheritedImpl()<!>

<!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>class T : JavaWithWrongInheritedImplGeneric<CharSequence>()<!>

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, javaType,
nullableType, operator, override */
