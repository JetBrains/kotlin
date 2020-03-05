// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

enum class Color {
    RED {
        fun <T : RED> simpleName(): RED = null!!
    }
}

class MyColor(val x: Color.RED, y: Color.RED) : Color.RED {

    var z: Color.RED = Color.RED
    set(arg: Color.RED) { z = arg }

    fun foo(arg: Color.RED): Color.RED = arg

    fun bar(): Color.RED {
        class Local : Color.RED
        fun local(arg: Color.RED): Color.RED = arg
        val temp: Color.RED = Color.RED
        temp as? Color.RED
        if (temp is Color.RED) {
        return temp as Color.RED
    }
        val obj = object : Color.RED {}
        if (obj is Color.RED) {
        return obj
    }
        return Color.RED
    }
}

fun create(): Array<Color.RED>? = null

interface Your<T : Color.RED>

class His : Your<Color.RED>

fun <T : Color.RED> otherCreate(): Array<T>? = null

typealias RedAlias = Color.RED

typealias ArrayOfEnumEntry = Array<Color.RED>

typealias ArrayOfEnumEntryAlias = Array<RedAlias>

fun <T> bar(a: Any): T = a as T

fun <T> foo() {
    foo<Color.RED>()
    foo<RedAlias>()
    <!INAPPLICABLE_CANDIDATE!>bar<!><Color.RED>(Color.RED)
}

fun Array<Color.RED>.foo(entries: Array<Color.RED>): Array<Color.RED> = null!!