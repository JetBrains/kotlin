val x by lazy { "1" }
    external get

class Delegate {
    operator fun getValue(thisRef: Any?, property: Any): String = ""
    operator fun setValue(thisRef: Any?, property: Any, value: String) {}
}

var y by Delegate()
    external get
    external set
