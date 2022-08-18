// FIR_IDENTICAL
@RequiresOptIn
annotation class Marker

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
class Final

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
sealed class SealedClass {
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
    object O : SealedClass()
}

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
sealed interface SealedInterface

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
fun interface FunInterface {
    fun doIt()
}

sealed class Normal

// Ok!
@SubclassOptInRequired(Marker::class)
open class NormalOpen : Normal()

// Ok!
@SubclassOptInRequired(Marker::class)
abstract class NormalAbstract : Normal()

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
sealed class ErrorSealed : Normal()

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
class ErrorFinal : Normal()

// Ok!
@SubclassOptInRequired(Marker::class)
abstract class Abstract

// Error! Should be replaced with OptIn
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
class Derived1 : Abstract()

// Ok!
@OptIn(Marker::class)
class Derived2 : Abstract()

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
object Obj

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
enum class E1 {
    FIRST;
}

enum class E2 {
    // Error!
    <!WRONG_ANNOTATION_TARGET!>@SubclassOptInRequired(Marker::class)<!>
    SECOND;
}

// Error!
<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
annotation class A

// Local stuff
fun foo() {
    // Error!
    val v = <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!> object : Any() {

    }

    // Error!
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
    open class OpenLocal

    // Ok!
    @OptIn(Marker::class)
    class DerivedLocal : OpenLocal()

    // Error!
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(Marker::class)<!>
    class Local
}

// Common rules with OptIn

annotation class Simple

// Error! Opt-in marker required
<!OPT_IN_ARGUMENT_IS_NOT_MARKER!>@SubclassOptInRequired(Simple::class)<!>
open class Some
