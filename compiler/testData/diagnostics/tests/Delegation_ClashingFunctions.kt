// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
trait One {
    public fun foo(): Any?
}
trait Two {
    public fun foo(): String?
}

trait Three {
    public fun foo(): String
}

class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test123<!>(val v1: One, val v2: Two, val v3: Three) : One by v1, Two by v2, Three by v3 { }
class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test132<!>(val v1: One, val v2: Two, val v3: Three) : One by v1, Three by v3, Two by v2 { }
class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test312<!>(val v1: One, val v2: Two, val v3: Three) : Three by v3, One by v1, Two by v2 { }
class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test321<!>(val v1: One, val v2: Two, val v3: Three) : Three by v3, Two by v2, One by v1 { }
class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test231<!>(val v1: One, val v2: Two, val v3: Three) : Two by v2, Three by v3, One by v1 { }
class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test213<!>(val v1: One, val v2: Two, val v3: Three) : Two by v2, One by v1, Three by v3 { }