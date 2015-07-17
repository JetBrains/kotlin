annotation class Ann

<!INAPPLICABLE_RECEIVER_TARGET, WRONG_ANNOTATION_TARGET!>@receiver:Ann<!>
class SomeClass {

    <!INAPPLICABLE_RECEIVER_TARGET, WRONG_ANNOTATION_TARGET!>@receiver:Ann<!>
    constructor(<!INAPPLICABLE_RECEIVER_TARGET!>@receiver:Ann<!> <!UNUSED_PARAMETER!>a<!>: String)

    <!INAPPLICABLE_RECEIVER_TARGET!>@receiver:Ann<!>
    protected val simpleProperty: String = "text"

    <!INAPPLICABLE_RECEIVER_TARGET!>@receiver:Ann<!>
    fun anotherFun() {
        <!INAPPLICABLE_RECEIVER_TARGET!>@receiver:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

    @receiver:Ann
    val String.extensionProperty2: String
        get() = "A"
}

@receiver:Ann
fun String.length2() = length()

@receiver:Ann
val String.extensionProperty: String
    get() = "A"