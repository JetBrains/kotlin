// WITH_STDLIB

// TODO separate bytecode text templates for FIR?
// -- CHECK_BYTECODE_TEXT
// -- JVM_IR_TEMPLATES
// -- 1 ASTORE 1
// -- 12 ALOAD 1
// -- JVM_TEMPLATES
// -- 2 ASTORE 1
// -- 13 ALOAD 1

// NB 'b' is evaluated before 's'
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun invokeNoInline(noinline b: (String) -> String, s: String) =
    b(s)

fun box(): String = invokeNoInline({ it + "K" }, "O")
