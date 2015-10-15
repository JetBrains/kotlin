import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) {}
}

<!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Ann<!>
class SomeClass {

    <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Ann<!>
    constructor()

    <!INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE!>@setparam:Ann<!>
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

    <!INAPPLICABLE_TARGET_ON_PROPERTY!>@setparam:Ann<!>
    fun anotherFun() {
        <!INAPPLICABLE_TARGET_ON_PROPERTY!>@setparam:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}
