val x by lazy { "1" }
    <!WRONG_EXTERNAL_DECLARATION!>external get<!>

class Delegate {
    operator fun getValue(thisRef: Any?, property: Any): String = ""
    operator fun setValue(thisRef: Any?, property: Any, value: String) {}
}

var y by Delegate()
    <!WRONG_EXTERNAL_DECLARATION!>external get<!>
    <!WRONG_EXTERNAL_DECLARATION!>external set<!>
