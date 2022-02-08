// LANGUAGE: -UseConsistentRulesForPrivateConstructorsOfSealedClasses
// ISSUE: KT-44866, KT-49729

// FILE: base.kt
sealed class SealedBase(x: Int) {
    private constructor(y: String) : this(y.length)

    class SealedNested : SealedBase("nested")
}
class SealedOuter : SealedBase(<!ARGUMENT_TYPE_MISMATCH!>"outer"<!>)

abstract class RegularBase(x: Int) {
    private constructor(y: String) : this(y.length)

    class RegularNested : RegularBase("nested")
}
class RegularOuter : RegularBase(<!ARGUMENT_TYPE_MISMATCH!>"outer"<!>)

// FILE: derived.kt

class SealedOuterInDifferentFile : SealedBase(<!ARGUMENT_TYPE_MISMATCH!>"other file"<!>)
