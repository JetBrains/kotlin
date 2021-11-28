
val a0: Any = 1u

val n0: Number = <!INITIALIZER_TYPE_MISMATCH!>1u<!>

val c0: Comparable<*> = 1u
val c1: Comparable<UInt> = 1u

val u0: UInt = 1u
val u1: UInt? = 1u
val u2: UInt? = u0
val u3: UInt? = u1

val i0: Int = <!INITIALIZER_TYPE_MISMATCH!>1u<!>

val m0 = <!UNRESOLVED_REFERENCE!>-<!>1u
val m1: UInt = <!UNRESOLVED_REFERENCE!>-<!>1u

val h1 = 0xFFu
val h2: UShort = 0xFFu

val b1 = 0b11u
val b2: UByte = 0b11u
