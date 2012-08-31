open data class D(private final val x: Int)

data class E(internal <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> val x: Int) : D(42)
