import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

<!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
class SomeClass {

    <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
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

    val useSiteTarget: Int
        <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING!>@get:Ann<!> get() = 5

    <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
    fun annotationOnFunction(a: Int) = a + 5

    fun anotherFun() {
        <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
        val localVariable = 5
    }

}
