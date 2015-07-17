annotation class Ann

class CustomDelegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
}

<!INAPPLICABLE_FIELD_TARGET, WRONG_ANNOTATION_TARGET!>@field:Ann<!>
class SomeClass {

    <!INAPPLICABLE_FIELD_TARGET, WRONG_ANNOTATION_TARGET!>@field:Ann<!>
    constructor()

    @field:Ann
    protected val simpleProperty: String = "text"

    @field:[Ann]
    protected val simplePropertyWithAnnotationList: String = "text"

    @field:Ann
    protected val delegatedProperty: String by CustomDelegate()

    <!INAPPLICABLE_FIELD_TARGET_NO_BACKING_FIELD!>@field:Ann<!>
    val propertyWithCustomGetter: Int
        get() = 5

    <!INAPPLICABLE_FIELD_TARGET, WRONG_ANNOTATION_TARGET!>@field:Ann<!>
    fun anotherFun(<!INAPPLICABLE_FIELD_TARGET!>@field:Ann<!> <!UNUSED_PARAMETER!>s<!>: String) {
        <!INAPPLICABLE_FIELD_TARGET!>@field:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}

class WithPrimaryConstructor(<!INAPPLICABLE_FIELD_TARGET!>@field:Ann<!> val a: String)