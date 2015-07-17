annotation class Ann

class CustomDelegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
    fun set(thisRef: Any?, prop: PropertyMetadata, value: String) {}
}

<!INAPPLICABLE_SPARAM_TARGET, WRONG_ANNOTATION_TARGET!>@sparam:Ann<!>
class SomeClass {

    <!INAPPLICABLE_SPARAM_TARGET, WRONG_ANNOTATION_TARGET!>@sparam:Ann<!>
    constructor()

    <!INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE!>@sparam:Ann<!>
    protected val simpleProperty: String = "text"

    @sparam:Ann
    protected var mutableProperty: String = "text"

    @sparam:[Ann]
    protected var mutablePropertyWithAnnotationList: String = "text"

    @sparam:Ann
    protected var delegatedProperty: String by CustomDelegate()

    @sparam:Ann
    var propertyWithCustomSetter: Int
        get() = 5
        set(v) {}

    <!INAPPLICABLE_SPARAM_TARGET!>@sparam:Ann<!>
    fun anotherFun() {
        <!INAPPLICABLE_SPARAM_TARGET!>@sparam:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}