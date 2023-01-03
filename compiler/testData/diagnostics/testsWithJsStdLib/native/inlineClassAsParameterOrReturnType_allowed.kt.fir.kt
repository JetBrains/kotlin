// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses, +JsAllowValueClassesInExternals
// !DIAGNOSTICS: +INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING

// FILE: uint.kt

package kotlin

inline class UInt(private val i: Int)

// FILE: test.kt

inline class SomeIC(val a: Int)

external val l: SomeIC

external val ll
    get(): SomeIC = definedExternally

external var r: SomeIC

external var rr: SomeIC
    get() = definedExternally
    set(v: SomeIC) { definedExternally }

external fun foo(): SomeIC
external fun foo(c: SomeIC): SomeIC
external fun foo(a: Int, c: SomeIC): SomeIC

external fun foo(a: Int, <!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> args: SomeIC)
external fun foo(a: Int, ui: UInt, vararg args: UInt)

external class CC(
    a: SomeIC,
    val b: SomeIC,
    var c: SomeIC
) {
    val l: SomeIC
    var r: SomeIC

    fun foo(): SomeIC
    fun foo(c: SomeIC): SomeIC
    fun foo(a: Int, c: SomeIC): SomeIC

    class N(
        a: SomeIC,
        val b: SomeIC,
        var c: SomeIC
    ) {
        val l: SomeIC
        var r: SomeIC

        fun foo(): SomeIC
        fun foo(c: SomeIC): SomeIC
        fun foo(a: Int, c: SomeIC): SomeIC
    }
}

external interface EI {
    val l: SomeIC
    var r: SomeIC

    fun foo(): SomeIC
    fun foo(c: SomeIC): SomeIC
    fun foo(a: Int, c: SomeIC): SomeIC
}
