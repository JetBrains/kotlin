annotation class Ann
annotation class Second

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:Ann<!>
class SomeClass {

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:Ann<!>
    constructor(@param:Ann a: String)

    @param:Ann
    protected val simpleProperty: String = "text"

    @param:Ann
    fun anotherFun() {
        @param:Ann
        val localVariable = 5
    }

}

class PrimaryConstructorClass(
        @param:Ann a: String,
@param:[Ann Second] b: String,
@param:Ann val c: String)