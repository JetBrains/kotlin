// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

enum class Color {
    RED {
        fun <T : <!OTHER_ERROR!>RED<!>> simpleName(): <!OTHER_ERROR!>RED<!> = null!!
    }
}

class MyColor(val x: <!OTHER_ERROR!>Color.RED<!>, y: <!OTHER_ERROR!>Color.RED<!>) : <!OTHER_ERROR!>Color.RED<!> {

    var z: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Color.RED<!> = Color.RED
    set(arg: Color.RED) { z = arg }

    fun foo(arg: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Color.RED<!>): <!OTHER_ERROR!>Color.RED<!> = arg

    fun bar(): <!OTHER_ERROR!>Color.RED<!> {
        class Local : <!OTHER_ERROR!>Color.RED<!>
        fun local(arg: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Color.RED<!>): <!OTHER_ERROR!>Color.RED<!> = arg
        val temp: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Color.RED<!> = Color.RED
        temp as? <!OTHER_ERROR!>Color.RED<!>
        if (temp is <!OTHER_ERROR!>Color.RED<!>) {
        return temp as <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Color.RED<!>
    }
        val obj = object : <!OTHER_ERROR!>Color.RED<!> {}
        if (obj is <!OTHER_ERROR!>Color.RED<!>) {
        return obj
    }
        return Color.RED
    }
}

fun create(): <!OTHER_ERROR!>Array<Color.RED>?<!> = null

interface Your<T : <!OTHER_ERROR!>Color.RED<!>>

class His : <!OTHER_ERROR!>Your<Color.RED><!>

fun <T : <!OTHER_ERROR!>Color.RED<!>> otherCreate(): Array<T>? = null

typealias RedAlias = <!OTHER_ERROR!>Color.RED<!>

typealias ArrayOfEnumEntry = <!OTHER_ERROR!>Array<Color.RED><!>

typealias ArrayOfEnumEntryAlias = Array<RedAlias>

fun <T> bar(a: Any): T = a as T

fun <T> foo() {
    foo<<!OTHER_ERROR, UPPER_BOUND_VIOLATED!>Color.RED<!>>()
    foo<RedAlias>()
    <!INAPPLICABLE_CANDIDATE!>bar<!><Color.RED>(Color.RED)
}

fun <!OTHER_ERROR!>Array<Color.RED><!>.foo(entries: <!OTHER_ERROR!>Array<Color.RED><!>): <!OTHER_ERROR!>Array<Color.RED><!> = null!!