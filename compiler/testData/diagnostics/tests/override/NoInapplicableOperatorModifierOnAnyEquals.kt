// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing error: com.android.tools.r8.errors.b: Class java.lang.Object cannot extend itself
// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE

package kotlin

open class Any() {
    public open operator fun equals(other: Any?): Boolean = TODO()

    public open fun hashCode(): Int = TODO()

    public open fun toString(): String = TODO()
}