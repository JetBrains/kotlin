package test

import org.jetbrains.annotations.NotNull

public trait TwoSuperclassesReturnSameJavaType: Object {
    public fun foo(): CharSequence?

    public trait Other: Object {
        public fun foo(): CharSequence
    }

    public trait Sub: TwoSuperclassesReturnSameJavaType, Other {
        override fun foo(): CharSequence
    }
}