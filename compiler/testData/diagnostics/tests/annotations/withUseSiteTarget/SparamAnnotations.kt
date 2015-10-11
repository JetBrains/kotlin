annotation class Ann

class CustomDelegate {
    public fun getValue(thisRef: Any?, prop: PropertyMetadata): String = prop.name
    fun setValue(thisRef: Any?, prop: PropertyMetadata, value: String) {}
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