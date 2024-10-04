// ISSUE: KT-15672
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT

enum class MyEnum {
    VALUE;

    init {
        println(<!UNINITIALIZED_ENUM_COMPANION!>staticValue<!>)
    }

    companion object {
        private val staticValue = VALUE
    }
}
