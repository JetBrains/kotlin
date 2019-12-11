import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) {}
}

@setparam:Ann
class SomeClass {

    @setparam:Ann
    constructor()

    @setparam:Ann
    protected val simpleProperty: String = "text"

    @setparam:Ann
    protected var mutableProperty: String = "text"

    @setparam:[Ann]
    protected var mutablePropertyWithAnnotationList: String = "text"

    @setparam:Ann
    protected var delegatedProperty: String by CustomDelegate()

    @setparam:Ann
    var propertyWithCustomSetter: Int
        get() = 5
        set(v) {}

    @setparam:Ann
    fun anotherFun() {
        @setparam:Ann
        val localVariable = 5
    }

}
