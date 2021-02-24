object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

lateinit var kest by Delegate

class A {
    lateinit val fest = "10"
    lateinit var mest: String
    lateinit var xest: String?
    lateinit var nest: Int
    lateinit val dest: String
        get() = "KEKER"
}

class B<T> {
    lateinit var best: T
}

fun rest() {
    lateinit var a: A
    lateinit var b: B<String> = B()
}