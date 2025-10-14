external interface I

external object O : I


class Delegate {
    operator fun getValue(thisRef: Any?, property: Any): String = ""

    operator fun setValue(thisRef: Any?, property: Any, value: String) {}
}

external class A : <!EXTERNAL_DELEGATION!>I<!> by O {
    val prop by <!EXTERNAL_DELEGATION!>Delegate()<!>

    var mutableProp by <!EXTERNAL_DELEGATION!>Delegate()<!>
}

external val topLevelProp by <!EXTERNAL_DELEGATION!>Delegate()<!>

val x by lazy { "1" }
    <!WRONG_EXTERNAL_DECLARATION!>external get<!>

var y by Delegate()
    <!WRONG_EXTERNAL_DECLARATION!>external get<!>
    <!WRONG_EXTERNAL_DECLARATION!>external set<!>
