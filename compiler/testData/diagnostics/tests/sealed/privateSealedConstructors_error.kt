// LANGUAGE: +UseConsistentRulesForPrivateConstructorsOfSealedClasses
// ISSUE: KT-44866, KT-49729

// FILE: base.kt
sealed class SealedBase(x: Int) {
    private constructor(y: String) : this(y.length)

    class SealedNested : SealedBase("nested")
}
class SealedOuter : <!INVISIBLE_MEMBER!>SealedBase<!>("outer")

abstract class RegularBase(x: Int) {
    private constructor(y: String) : this(y.length)

    class RegularNested : RegularBase("nested")
}
class RegularOuter : <!INVISIBLE_MEMBER!>RegularBase<!>("outer")

// FILE: derived.kt

class SealedOuterInDifferentFile : <!INVISIBLE_MEMBER!>SealedBase<!>("other file")
