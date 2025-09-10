// WITH_STDLIB
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun foo() { }

class Foo {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    inline fun foo() { }
}
