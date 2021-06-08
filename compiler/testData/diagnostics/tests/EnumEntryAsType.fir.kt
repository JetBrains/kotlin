// !DIAGNOSTICS: -UNUSED_PARAMETER

enum class Color {
    RED {
        fun <T : <!UNRESOLVED_REFERENCE!>RED<!>> simpleName(): <!UNRESOLVED_REFERENCE!>RED<!> = null!!
    }
}

class MyColor(val x: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>, y: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>) : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> {

    var z: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = Color.RED
    set(arg: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>) { z = arg }

    fun foo(arg: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>): <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = arg

    fun bar(): <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> {
        class Local : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>
        fun local(arg: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>): <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = arg
        val temp: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = Color.RED
        temp as? <!ENUM_ENTRY_AS_TYPE, ENUM_ENTRY_AS_TYPE!>Color.RED<!>
        if (temp is <!IS_ENUM_ENTRY!>Color.RED<!>) {
        return temp as <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>
    }
        val obj = object : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> {}
        if (obj is <!IS_ENUM_ENTRY!>Color.RED<!>) {
        return obj
    }
        return Color.RED
    }
}

fun create(): Array<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>? = null

interface Your<T : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>

class His : Your<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>

fun <T : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>> otherCreate(): Array<T>? = null

typealias RedAlias = <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>

typealias ArrayOfEnumEntry = Array<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>

typealias ArrayOfEnumEntryAlias = Array<RedAlias>

fun <T> bar(a: Any): T = a as T

fun <T> foo() {
    foo<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>()
    foo<<!CANNOT_INFER_PARAMETER_TYPE!>RedAlias<!>>()
    bar<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>(<!ARGUMENT_TYPE_MISMATCH!>Color.RED<!>)
}

fun Array<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>.foo(entries: Array<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>): Array<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>> = null!!
