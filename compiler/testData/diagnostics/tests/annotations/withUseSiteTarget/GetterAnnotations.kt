import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
class SomeClass {

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
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

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
    fun annotationOnFunction(a: Int) = a + 5

    fun anotherFun() {
        <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}
