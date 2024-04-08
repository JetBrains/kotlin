// RUN_PIPELINE_TILL: BACKEND

sealed class EffectiveVisibility(val name: String, val publicApi: Boolean = false, val privateApi: Boolean = false) {
    override fun toString() = name

    sealed class InternalOrPackage(internal: Boolean) : EffectiveVisibility(
        if (internal) "internal" else "public/*package*/"
    ) {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
                is InternalOrPackage -> Permissiveness.SAME
                ProtectedBound, is Protected, Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility): EffectiveVisibility =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalOrPackage, is InternalProtected -> other
                is Protected -> InternalProtected(other.containerTypeConstructor)
                is Unknown -> Local
                ProtectedBound -> InternalProtectedBound
            }
    }

    /**
     * A declaration with this visibility is generally visible only within the same module, or from friend modules.
     */
    object Internal : InternalOrPackage(true)

    /**
     * The default visibility in Java, if no visibility is specified explicitly.
     * A declaration with this visibility is visible from other modules, but only within the same package.
     */
    object PackagePrivate : InternalOrPackage(false)

    /**
     * Represents a private class/interface member.
     */
    object PrivateInClass : EffectiveVisibility("private-in-class", privateApi = true) {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            if (this == other || Local == other) Permissiveness.SAME else Permissiveness.LESS
    }

    /**
     * Represents a local class/object/interface member, effectively the same as [PrivateInClass].
     */
    object Local : EffectiveVisibility("local") {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            if (this == other || PrivateInClass == other) Permissiveness.SAME else Permissiveness.LESS
    }

    /**
     * Reflects the `CANNOT_INFER_VISIBILITY` diagnostic.
     */
    object Unknown : EffectiveVisibility("unknown") {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            if (other == Unknown)
                Permissiveness.SAME
            else
                Permissiveness.UNKNOWN
    }

    /**
     * A declaration with this visibility is only visible within the file it's declared in.
     */
    object PrivateInFile : EffectiveVisibility("private-in-file", privateApi = true) {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            when (other) {
                this -> Permissiveness.SAME
                PrivateInClass, Local -> Permissiveness.MORE
                else -> Permissiveness.LESS
            }
    }

    /**
     * The broadest visibility possible in the language.
     */
    object Public : EffectiveVisibility("public", publicApi = true) {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            when (other) {
                this -> Permissiveness.SAME
                Unknown -> Permissiveness.UNKNOWN
                else -> Permissiveness.MORE
            }
    }

    /**
     * This visibility is effectively public, in the sense that declarations with this visibility can be referenced from other modules.
     *
     * @property containerTypeConstructor The class from whose subclasses a declaration with this visibility is visible.
     */
    class Protected(val containerTypeConstructor: String?) : EffectiveVisibility("protected", publicApi = true) {

        override fun equals(other: Any?) = (other is Protected && containerTypeConstructor == other.containerTypeConstructor)

        override fun hashCode() = containerTypeConstructor?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerTypeConstructor ?: '?'})"

        override fun relation(other: EffectiveVisibility): Permissiveness =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
                is Protected -> Permissiveness.UNKNOWN
                is InternalProtected -> Permissiveness.UNKNOWN
                is InternalOrPackage, is Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility): EffectiveVisibility =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is Protected -> when (relation(other)) {
                    Permissiveness.SAME, Permissiveness.LESS -> this
                    Permissiveness.MORE -> other
                    Permissiveness.UNKNOWN -> ProtectedBound
                }
                is InternalProtected -> when (relation(other)) {
                    Permissiveness.MORE -> other
                    else -> InternalProtectedBound
                }
                is InternalOrPackage -> InternalProtected(containerTypeConstructor)
                is Unknown -> Local
            }
    }

    /**
     * The lower bound for all protected visibilities.
     */
    object ProtectedBound : EffectiveVisibility("protected (in different classes)", publicApi = true) {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public, is Protected -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> Permissiveness.MORE
                ProtectedBound -> Permissiveness.SAME
                is InternalOrPackage, is InternalProtected, is Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility): EffectiveVisibility =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public, is Protected -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is InternalOrPackage, is InternalProtected -> InternalProtectedBound
                is Unknown -> Local
            }
    }

    /**
     * The lower bound for [Internal] and [Protected].
     *
     * @property containerTypeConstructor See [Protected.containerTypeConstructor].
     */
    class InternalProtected(
        val containerTypeConstructor: String?
    ) : EffectiveVisibility("internal & protected", publicApi = false) {

        override fun equals(other: Any?) = (other is InternalProtected && containerTypeConstructor == other.containerTypeConstructor)

        override fun hashCode() = containerTypeConstructor?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerTypeConstructor ?: '?'})"

        override fun relation(other: EffectiveVisibility): Permissiveness =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public, is InternalOrPackage -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> Permissiveness.MORE
                is InternalProtected -> Permissiveness.UNKNOWN
                is Protected -> Permissiveness.UNKNOWN
                ProtectedBound, Unknown -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility): EffectiveVisibility =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public, is InternalOrPackage -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> other
                is Protected, is InternalProtected -> when (relation(other)) {
                    Permissiveness.SAME, Permissiveness.LESS -> this
                    Permissiveness.MORE -> other
                    Permissiveness.UNKNOWN -> InternalProtectedBound
                }
                ProtectedBound -> InternalProtectedBound
                Unknown -> Local
            }
    }

    /**
     * The lower bound for [Internal] and [ProtectedBound].
     */
    object InternalProtectedBound : EffectiveVisibility("internal & protected (in different classes)") {
        override fun relation(other: EffectiveVisibility): Permissiveness =
            <!UNSAFE_EXHAUSTIVENESS!>when<!> (other) {
                Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local -> Permissiveness.MORE
                InternalProtectedBound -> Permissiveness.SAME
                Unknown -> Permissiveness.UNKNOWN
            }
    }

    enum class Permissiveness {
        LESS,
        SAME,
        MORE,
        UNKNOWN
    }

    /**
     * Calculates whether `this` is more visible or less visible then [other].
     */
    abstract fun relation(other: EffectiveVisibility): Permissiveness

    /**
     * Returns the most permissive visibility that is **not** more visible than both `this` and [other].
     */
    open fun lowerBound(other: EffectiveVisibility): EffectiveVisibility =
        when (relation(other)) {
            Permissiveness.SAME, Permissiveness.LESS -> this
            Permissiveness.MORE -> other
            Permissiveness.UNKNOWN -> PrivateInClass
        }
}
