annotation class Ann
annotation class Second

<!INAPPLICABLE_PARAM_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:Ann<!>
class SomeClass {

    <!INAPPLICABLE_PARAM_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:Ann<!>
    constructor(<!INAPPLICABLE_PARAM_TARGET!>@param:Ann<!> <!UNUSED_PARAMETER!>a<!>: String)

    <!INAPPLICABLE_PARAM_TARGET!>@param:Ann<!>
    protected val simpleProperty: String = "text"

    <!INAPPLICABLE_PARAM_TARGET!>@param:Ann<!>
    fun anotherFun() {
        <!INAPPLICABLE_PARAM_TARGET!>@param:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}

class PrimaryConstructorClass(
        <!REDUNDANT_ANNOTATION_TARGET!>@param:Ann<!> <!UNUSED_PARAMETER!>a<!>: String,
@param:[<!REDUNDANT_ANNOTATION_TARGET!>Ann<!> <!REDUNDANT_ANNOTATION_TARGET!>Second<!>] <!UNUSED_PARAMETER!>b<!>: String,
@param:Ann val c: String)