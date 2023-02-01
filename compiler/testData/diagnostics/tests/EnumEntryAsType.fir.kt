// !DIAGNOSTICS: -UNUSED_PARAMETER

enum class Color {
    RED {
        fun <T : <!UNRESOLVED_REFERENCE!>RED<!>> simpleName(): <!UNRESOLVED_REFERENCE!>RED<!> = null!!
    }
}

class MyColor(val x: Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>, y: Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>) : Color.<!ENUM_ENTRY_AS_TYPE!>RED<!> {

    var z: Color.<!ENUM_ENTRY_AS_TYPE!>RED<!> = Color.RED
    set(arg: Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>) { z = arg }

    fun foo(arg: Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>): Color.<!ENUM_ENTRY_AS_TYPE!>RED<!> = arg

    fun bar(): Color.<!ENUM_ENTRY_AS_TYPE!>RED<!> {
        class Local : Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>
        fun local(arg: Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>): Color.<!ENUM_ENTRY_AS_TYPE!>RED<!> = arg
        val temp: Color.<!ENUM_ENTRY_AS_TYPE!>RED<!> = Color.RED
        temp as? Color.<!ENUM_ENTRY_AS_TYPE, ENUM_ENTRY_AS_TYPE!>RED<!>
        if (temp is <!IS_ENUM_ENTRY!>Color.RED<!>) {
        return temp as Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>
    }
        val obj = object : Color.<!ENUM_ENTRY_AS_TYPE!>RED<!> {}
        if (obj is <!IS_ENUM_ENTRY!>Color.RED<!>) {
        return obj
    }
        return Color.RED
    }
}

fun create(): Array<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>? = null

interface Your<T : Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>

class His : Your<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>

fun <T : Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>> otherCreate(): Array<T>? = null

typealias RedAlias = Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>

typealias ArrayOfEnumEntry = Array<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>

typealias ArrayOfEnumEntryAlias = Array<RedAlias>

fun <T> bar(a: Any): T = a <!UNCHECKED_CAST!>as T<!>

fun <T> foo() {
    foo<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>()
    foo<<!CANNOT_INFER_PARAMETER_TYPE!>RedAlias<!>>()
    bar<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>(<!ARGUMENT_TYPE_MISMATCH!>Color.RED<!>)
}

fun Array<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>.foo(entries: Array<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>>): Array<Color.<!ENUM_ENTRY_AS_TYPE!>RED<!>> = null!!
