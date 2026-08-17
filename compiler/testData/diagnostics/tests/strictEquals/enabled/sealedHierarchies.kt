// RUN_PIPELINE_TILL: FRONTEND

object Unrelated

sealed interface EitherNonData {
    override fun equals(@EqualityBound(EitherNonData::class) other: Any?): Boolean

    <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Left<!> : EitherNonData
    <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Right<!> : EitherNonData
}

sealed interface Either {
    override fun equals(@EqualityBound(Either::class) other: Any?): Boolean

    data class Left(val a: String) : Either
    data class Right(val b: String) : Either

    fun test(either: Either, left: Left, right: Right, otherLeft: Left, otherRight: Right) {
        if (either == left) {}
        if (left == either) {}
        if (either == right) {}
        if (right == either) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>either == Unrelated<!>) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == either<!>) {}

        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>left == right<!>) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>right == left<!>) {}

        if (left == otherLeft) {}
        if (right == otherRight) {}
    }
}

sealed interface EitherLeftSpecial {
    override fun equals(@EqualityBound(EitherLeftSpecial::class) other: Any?): Boolean

    class Left : EitherLeftSpecial {
        override fun equals(@EqualityBound(EitherLeftSpecial::class) other: Any?): Boolean = true
    }

    data class Right(val b: String) : EitherLeftSpecial

    fun test(either: EitherLeftSpecial, left: Left, right: Right, otherLeft: Left, otherRight: Right) {
        if (either == left) {}
        if (left == either) {}
        if (either == right) {}
        if (right == either) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>either == Unrelated<!>) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == either<!>) {}

        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>left == right<!>) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>right == left<!>) {}

        if (left == otherLeft) {}
        if (right == otherRight) {}
    }
}

sealed interface EitherLeftSpecific {
    override fun equals(@EqualityBound(EitherLeftSpecific::class) other: Any?): Boolean

    class Left : EitherLeftSpecific {
        override fun equals(@EqualityBound(Left::class) other: Any?): Boolean = true
    }

    data class Right(val b: String) : EitherLeftSpecific

    fun test(either: EitherLeftSpecific, left: Left, right: Right, otherLeft: Left, otherRight: Right) {
        if (either == left) {}
        if (left == either) {}
        if (either == right) {}
        if (right == either) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>either == Unrelated<!>) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == either<!>) {}

        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>left == right<!>) {}
        if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>right == left<!>) {}

        if (left == otherLeft) {}
        if (right == otherRight) {}
    }
}
