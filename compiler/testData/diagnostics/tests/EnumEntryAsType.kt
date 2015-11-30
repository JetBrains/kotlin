// !DIAGNOSTICS: -UNUSED_PARAMETER

enum class Color { RED }

class MyColor(val x: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>, y: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>) : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> {

    var z: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = <!TYPE_MISMATCH!>Color.RED<!>
        set(arg: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>) { z = arg }

    fun foo(arg: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>): <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = arg

    fun bar(): <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> {
        class Local : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>
        fun local(arg: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>): <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = arg
        val temp: <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> = <!TYPE_MISMATCH!>Color.RED<!>
        temp <!USELESS_CAST!>as? <!ENUM_ENTRY_AS_TYPE!>Color.RED<!><!>
        if (temp is <!IS_ENUM_ENTRY!>Color.RED<!>) {
            return temp <!USELESS_CAST!>as <!ENUM_ENTRY_AS_TYPE!>Color.RED<!><!>
        }
        val obj = object : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!> {}
        if (obj is <!IS_ENUM_ENTRY!>Color.RED<!>) {
            return obj
        }
        return <!TYPE_MISMATCH!>Color.RED<!>
    }
}

fun create(): Array<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>? = null

interface Your<T : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>

class His : Your<<!ENUM_ENTRY_AS_TYPE!>Color.RED<!>>

fun <T : <!ENUM_ENTRY_AS_TYPE!>Color.RED<!>> otherCreate(): Array<T>? = null