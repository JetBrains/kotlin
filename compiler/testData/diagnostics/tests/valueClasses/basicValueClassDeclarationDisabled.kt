// FIR_IDENTICAL
// !SKIP_JAVAC
// !API_VERSION: 1.4
// !LANGUAGE: -InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin.jvm

annotation class JvmInline

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>value<!> annotation class InlineAnn
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object InlineObject
<!WRONG_MODIFIER_TARGET!>value<!> enum class InlineEnum

@JvmInline
value class NotVal(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)
