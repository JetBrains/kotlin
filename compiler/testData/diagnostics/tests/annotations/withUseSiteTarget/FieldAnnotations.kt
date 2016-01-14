import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
class SomeClass {

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    constructor()

    @field:Ann
    protected val simpleProperty: String = "text"

    @field:[Ann]
    protected val simplePropertyWithAnnotationList: String = "text"

    <!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD!>@field:Ann<!>
    protected val delegatedProperty: String by CustomDelegate()

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    val propertyWithCustomGetter: Int
        get() = 5

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    fun anotherFun(<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!> <!UNUSED_PARAMETER!>s<!>: String) {
        <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}

class WithPrimaryConstructor(@field:Ann val a: String)
