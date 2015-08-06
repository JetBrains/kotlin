annotation class Ann

class CustomDelegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
    fun set(thisRef: Any?, prop: PropertyMetadata, value: String) {}
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