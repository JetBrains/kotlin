package test

import org.jetbrains.annotations.NotNull

public trait TwoSuperclassesReturnJavaSubtype: Object {
    public fun foo(): CharSequence?

    public trait Other: Object {
        public fun foo(): CharSequence
    }

    public trait Sub: TwoSuperclassesReturnJavaSubtype, Other {
        override fun foo(): String
    }
}