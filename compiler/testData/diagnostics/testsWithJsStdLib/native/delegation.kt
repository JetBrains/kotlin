external interface I

external object O : I


class Delegate {
    operator fun getValue(thisRef: Any?, property: Any): String = ""

    operator fun setValue(thisRef: Any?, property: Any, value: String) {}
}

external class A : <!EXTERNAL_DELEGATION!>I by O<!> {
    val prop <!EXTERNAL_DELEGATION!>by Delegate()<!>

    var mutableProp <!EXTERNAL_DELEGATION!>by Delegate()<!>
}

external val topLevelProp <!EXTERNAL_DELEGATION!>by Delegate()<!>

val x <!EXTERNAL_DELEGATION!>by lazy { "1" }<!>
    <!WRONG_EXTERNAL_DECLARATION!>external get<!>

var y <!EXTERNAL_DELEGATION!>by Delegate()<!>
    <!WRONG_EXTERNAL_DECLARATION!>external get<!>
    <!WRONG_EXTERNAL_DECLARATION!>external set<!>
