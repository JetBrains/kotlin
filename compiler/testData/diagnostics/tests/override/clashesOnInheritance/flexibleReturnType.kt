// FILE: J.java
public interface J {
    String foo(); // String!
}

// FILE: K.kt
interface K1 {
    fun foo(): String
}

interface K2 {
    fun foo(): String?
}

interface KDerived1a : K1, J

interface KDerived1b : J, K1

interface KDerived2a : K2, J

interface KDerived2b : J, K2

interface KDerived12a : K1, K2, J
// public abstract override /*3*/ /*fake_override*/ fun foo(): kotlin.String!

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>interface KDerived12b<!> : K1, J, K2 // TODO
// public abstract override /*3*/ /*fake_override*/ fun foo(): kotlin.String?

interface KDerived12c : J, K1, K2
// public abstract override /*3*/ /*fake_override*/ fun foo(): kotlin.String