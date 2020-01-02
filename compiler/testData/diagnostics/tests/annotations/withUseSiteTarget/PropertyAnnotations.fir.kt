import kotlin.reflect.KProperty

annotation class Ann
annotation class Second

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

@property:Ann
class SomeClass {

    @property:Ann
    constructor(s: String)

    @property:Ann
    protected val p1: String = ""

    @property:[Ann Second]
    protected val p2: String = ""

    @property:Ann
    protected var p3: String = ""

    @property:Ann
    protected val p4: String by CustomDelegate()

    @property:Ann
    var propertyWithCustomSetter: Int
        get() = 5
        set(v) {}

    @property:Ann
    fun anotherFun() {
        @property:Ann
        val localVariable = 5
    }

}
