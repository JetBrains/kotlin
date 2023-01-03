// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses

external inline class C(val a: Int) {
    fun foo()
}

<!WRONG_MODIFIER_TARGET!>inline<!> external enum class E {
    A
}
