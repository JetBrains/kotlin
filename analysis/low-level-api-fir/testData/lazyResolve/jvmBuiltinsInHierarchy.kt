// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ALLOW_KOTLIN_PACKAGE
// EXPECT_BUILTINS_AS_PART_OF_STDLIB

// FILE: Any.kt
@file:kotlin.internal.JvmBuiltin
@file:kotlin.internal.SuppressBytecodeGeneration
@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")

package kotlin

public actual open class Any {
    public actual open operator fun equals(other: Any?): Boolean
    public actual open fun hashCode(): Int
    public actual open fun toString(): String
}

// FILE: main.kt
abstract class MyMutableSet<E> : AbstractMutableSet<E>() {
    override val si<caret>ze: Int get() = 0
}
