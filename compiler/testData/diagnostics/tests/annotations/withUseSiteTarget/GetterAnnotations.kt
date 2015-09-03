annotation class Ann

class CustomDelegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
}

<!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
class SomeClass {

    <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
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

    <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
    fun annotationOnFunction(a: Int) = a + 5

    fun anotherFun() {
        <!INAPPLICABLE_TARGET_ON_PROPERTY!>@get:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}