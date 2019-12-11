// !LANGUAGE: +LateinitTopLevelProperties

object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

lateinit var testOk: String

lateinit val testErr0: Any
lateinit var testErr1: Int
lateinit var testErr2: Any?
lateinit var testErr3: String = ""
lateinit var testErr4 by Delegate
