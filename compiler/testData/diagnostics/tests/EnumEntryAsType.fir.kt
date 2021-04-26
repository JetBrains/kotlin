// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

enum class Color {
    RED {
        fun <T : <!UNRESOLVED_REFERENCE!>RED<!>> simpleName(): <!UNRESOLVED_REFERENCE!>RED<!> = null!!
    }
}

class MyColor(val x: <!UNRESOLVED_REFERENCE!>Color.RED<!>, y: <!UNRESOLVED_REFERENCE!>Color.RED<!>) : <!UNRESOLVED_REFERENCE!>Color.RED<!> {

    var z: <!UNRESOLVED_REFERENCE!>Color.RED<!> = Color.RED
    set(arg: <!UNRESOLVED_REFERENCE!>Color.RED<!>) { z = arg }

    fun foo(arg: <!UNRESOLVED_REFERENCE!>Color.RED<!>): <!UNRESOLVED_REFERENCE!>Color.RED<!> = arg

    fun bar(): <!UNRESOLVED_REFERENCE!>Color.RED<!> {
        class Local : <!UNRESOLVED_REFERENCE!>Color.RED<!>
        fun local(arg: <!UNRESOLVED_REFERENCE!>Color.RED<!>): <!UNRESOLVED_REFERENCE!>Color.RED<!> = arg
        val temp: <!UNRESOLVED_REFERENCE!>Color.RED<!> = Color.RED
        temp as? <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Color.RED<!>
        if (temp is <!UNRESOLVED_REFERENCE!>Color.RED<!>) {
        return temp as <!UNRESOLVED_REFERENCE!>Color.RED<!>
    }
        val obj = object : <!UNRESOLVED_REFERENCE!>Color.RED<!> {}
        if (obj is <!UNRESOLVED_REFERENCE!>Color.RED<!>) {
        return obj
    }
        return Color.RED
    }
}

fun create(): Array<<!UNRESOLVED_REFERENCE!>Color.RED<!>>? = null

interface Your<T : <!UNRESOLVED_REFERENCE!>Color.RED<!>>

class His : Your<<!UNRESOLVED_REFERENCE!>Color.RED<!>>

fun <T : <!UNRESOLVED_REFERENCE!>Color.RED<!>> otherCreate(): Array<T>? = null

typealias RedAlias = <!UNRESOLVED_REFERENCE!>Color.RED<!>

typealias ArrayOfEnumEntry = Array<<!UNRESOLVED_REFERENCE!>Color.RED<!>>

typealias ArrayOfEnumEntryAlias = Array<RedAlias>

fun <T> bar(a: Any): T = a as T

fun <T> foo() {
    foo<<!UNRESOLVED_REFERENCE!>Color.RED<!>>()
    foo<RedAlias>()
    bar<<!UNRESOLVED_REFERENCE!>Color.RED<!>>(<!ARGUMENT_TYPE_MISMATCH!>Color.RED<!>)
}

fun Array<<!UNRESOLVED_REFERENCE!>Color.RED<!>>.foo(entries: Array<<!UNRESOLVED_REFERENCE!>Color.RED<!>>): Array<<!UNRESOLVED_REFERENCE!>Color.RED<!>> = null!!
