annotation class Ann

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Ann<!>
class SomeClass {

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Ann<!>
    constructor(<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Ann<!> <!UNUSED_PARAMETER!>a<!>: String)

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Ann<!>
    protected val simpleProperty: String = "text"

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Ann<!>
    fun anotherFun() {
        <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

    val @receiver:Ann String.extensionProperty2: String
        get() = "A"
}

fun @receiver:Ann String.length2() = length

val @receiver:Ann String.extensionProperty: String
    get() = "A"