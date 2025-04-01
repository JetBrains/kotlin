// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ALLOW_KOTLIN_PACKAGE
// EXPECT_BUILTINS_AS_PART_OF_STDLIB

// FILE: Annotations.kt
package kotlin.internal

/**
 * Specifies that all file declarations are builtins and should be serialized to .kotlin_metadata
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmBuiltin

/**
 * Do not generate bytecode for declarations in the file (and therefore do not lower them)
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class SuppressBytecodeGeneration

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
