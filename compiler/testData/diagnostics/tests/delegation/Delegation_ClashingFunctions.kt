// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
interface One {
    public fun foo(): Any?
}
interface Two {
    public fun foo(): String?
}

interface Three {
    public fun foo(): String
}

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class Test123<!>(val v1: One, val v2: Two, val v3: Three) : One by v1, Two by v2, Three by v3 { }
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class Test132<!>(val v1: One, val v2: Two, val v3: Three) : One by v1, Three by v3, Two by v2 { }
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Test312<!>(val v1: One, val v2: Two, val v3: Three) : Three by v3, One by v1, Two by v2 { }
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Test321<!>(val v1: One, val v2: Two, val v3: Three) : Three by v3, Two by v2, One by v1 { }
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class Test231<!>(val v1: One, val v2: Two, val v3: Three) : Two by v2, Three by v3, One by v1 { }
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class Test213<!>(val v1: One, val v2: Two, val v3: Three) : Two by v2, One by v1, Three by v3 { }