import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) {}
}

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@set:Ann<!>
class SomeClass {

    @set:Ann
    protected val simpleProperty: String = "text"

    @set:Ann
    protected var mutableProperty: String = "text"

    @set:[Ann]
    protected var mutablePropertyWithAnnotationList: String = "text"

    @set:Ann
    protected var delegatedProperty: String by CustomDelegate()

    @set:Ann
    var propertyWithCustomSetter: Int
        get() = 5
        set(v) {}

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@set:Ann<!>
    fun annotationOnFunction(a: Int) = a + 5

    fun anotherFun() {
        @set:Ann
        val localVariable = 5
    }

}
