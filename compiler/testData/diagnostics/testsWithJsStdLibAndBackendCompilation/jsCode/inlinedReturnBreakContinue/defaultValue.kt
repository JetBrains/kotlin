// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
inline fun foo1(bar: () -> Unit = { 1 }) = js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"bar()"<!>)

inline fun foo2(x: Int, bar: () -> Unit = { x }) = js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"bar()"<!>)

inline fun foo3(noinline bar: () -> Unit, baz: () -> Unit = fun() = bar()) = js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"baz()"<!>)

inline fun foo4(crossinline bar: () -> Unit, noinline baz: () -> Unit = fun() = bar()) = js("baz()")
