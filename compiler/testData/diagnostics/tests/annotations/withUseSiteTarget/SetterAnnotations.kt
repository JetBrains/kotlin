import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) {}
}

<!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@set:Ann<!>
class SomeClass {

    <!INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE!>@set:Ann<!>
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

    <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@set:Ann<!>
    fun annotationOnFunction(a: Int) = a + 5

    fun anotherFun() {
        <!INAPPLICABLE_TARGET_ON_PROPERTY!>@set:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}
