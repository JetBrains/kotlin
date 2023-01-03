external interface I

external object O : I


class Delegate {
    operator fun getValue(thisRef: Any?, property: Any): String = ""

    operator fun setValue(thisRef: Any?, property: Any, value: String) {}
}

external class A : I by O {
    val prop by Delegate()

    var mutableProp by Delegate()
}

external val topLevelProp by Delegate()
