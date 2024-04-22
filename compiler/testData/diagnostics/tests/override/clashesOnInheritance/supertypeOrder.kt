// FIR_IDENTICAL
// ISSUE: KT-65090

// FILE: JavaInterface.java
public interface JavaInterface {
    public void bar(Object o);
}

// FILE: 1.kt

interface KotlinInterface {
    fun bar(o: Any)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class JK<!> : JavaInterface, KotlinInterface {
    override fun bar(o: Any?) { }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class KJ<!> : KotlinInterface , JavaInterface {
    override fun bar(o: Any?) { }
}
