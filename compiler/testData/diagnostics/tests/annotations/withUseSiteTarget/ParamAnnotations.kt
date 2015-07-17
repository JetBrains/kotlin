annotation class Ann
annotation class Second

<!INAPPLICABLE_SPARAM_TARGET, WRONG_ANNOTATION_TARGET!>@param:Ann<!>
class SomeClass {

    <!INAPPLICABLE_SPARAM_TARGET, WRONG_ANNOTATION_TARGET!>@param:Ann<!>
    constructor(<!INAPPLICABLE_SPARAM_TARGET!>@param:Ann<!> <!UNUSED_PARAMETER!>a<!>: String)

    <!INAPPLICABLE_SPARAM_TARGET!>@param:Ann<!>
    protected val simpleProperty: String = "text"

    <!INAPPLICABLE_SPARAM_TARGET!>@param:Ann<!>
    fun anotherFun() {
        <!INAPPLICABLE_SPARAM_TARGET!>@param:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}

class PrimaryConstructorClass(
        @param:Ann <!UNUSED_PARAMETER!>a<!>: String,
        @param:[Ann Second] <!UNUSED_PARAMETER!>b<!>: String,
        @param:Ann val <!UNUSED_PARAMETER!>c<!>: String)