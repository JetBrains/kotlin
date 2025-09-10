// WITH_STDLIB
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR

@file:[JvmName("Foo") JvmMultifileClass]
package test

fun foo() {
    inlineOnly()
}

// No method should be generated in multifile facade for 'inlineOnly'
// Because 'inlineOnly' is private in file part (because it's inline-only) and can't be delegated from facade
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun inlineOnly() { }
