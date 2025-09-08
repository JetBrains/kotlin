// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
inline fun foo1(bar: () -> Unit, noinline baz: () -> Unit) = js("baz()")

inline fun foo2(bar: () -> Unit, noinline baz: () -> Unit) = js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"baz(); bar()"<!>)

inline fun foo3(bar: () -> Unit, noinline baz: () -> Unit) = js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"baz(bar())"<!>)
