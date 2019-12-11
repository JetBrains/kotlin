import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

@get:Ann
class SomeClass {

    @get:Ann
    constructor()

    @get:Ann
    protected val simpleProperty: String = "text"

    @get:Ann
    protected var mutableProperty: String = "text"

    @get:[Ann]
    protected val simplePropertyWithAnnotationList: String = "text"

    @get:Ann
    protected val delegatedProperty: String by CustomDelegate()

    @get:Ann
    val propertyWithCustomGetter: Int
        get() = 5

    @get:Ann
    fun annotationOnFunction(a: Int) = a + 5

    fun anotherFun() {
        @get:Ann
        val localVariable = 5
    }

}
