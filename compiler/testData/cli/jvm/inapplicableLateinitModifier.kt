object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

lateinit var test: Int
lateinit var kest by Delegate

lateinit var good: String

class A {
    lateinit val fest = "10"
    lateinit var mest: String
    lateinit var xest: String?
    lateinit var nest: Int
    lateinit var west: Char
    lateinit var qest: Boolean
    lateinit var aest: Short
    lateinit var hest: Byte
    lateinit var jest: Long
    lateinit val dest: String
        get() = "KEKER"
}

class B<T> {
    lateinit var best: T
}

class C<K : Any> {
    lateinit var pest: K
    lateinit var vest: K?
}

fun rest() {
    lateinit var i: Int
    lateinit var a: A
    lateinit var b: B<String> = B()
}