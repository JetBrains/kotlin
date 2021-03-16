// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses

class A

<!INLINE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS!>context(A)<!>
inline class B(val x: Int)