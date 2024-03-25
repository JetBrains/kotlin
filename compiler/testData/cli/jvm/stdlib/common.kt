// ISSUE: KT-65841

package kotlin

@ActualizeByJvmBuiltinProvider
expect interface Annotation

annotation class ActualizeByJvmBuiltinProvider()

@ActualizeByJvmBuiltinProvider
expect open class Any() {
    public open operator fun equals(other: Any?): Boolean

    public open fun hashCode(): Int

    public open fun toString(): String
}

@ActualizeByJvmBuiltinProvider
expect class Boolean

@ActualizeByJvmBuiltinProvider
expect class Int

@ActualizeByJvmBuiltinProvider
expect class String
