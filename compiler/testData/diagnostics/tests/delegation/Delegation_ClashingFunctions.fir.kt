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

class Test123(val v1: One, val v2: Two, val v3: Three) : One by v1, Two by v2, Three by v3 { }
class Test132(val v1: One, val v2: Two, val v3: Three) : One by v1, Three by v3, Two by v2 { }
class Test312(val v1: One, val v2: Two, val v3: Three) : Three by v3, One by v1, Two by v2 { }
class Test321(val v1: One, val v2: Two, val v3: Three) : Three by v3, Two by v2, One by v1 { }
class Test231(val v1: One, val v2: Two, val v3: Three) : Two by v2, Three by v3, One by v1 { }
class Test213(val v1: One, val v2: Two, val v3: Three) : Two by v2, One by v1, Three by v3 { }