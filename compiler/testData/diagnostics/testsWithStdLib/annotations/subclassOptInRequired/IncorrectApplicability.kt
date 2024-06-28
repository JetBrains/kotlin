// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

// final classes are not valid targets for @SubclassOptInRequired

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
class Klass

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
data class DataKlass(val arg: Int)

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
@JvmInline value class ValueKlass(val arg: Int)

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
annotation class AnnotationKlass

// functional interface is not a valid target for @SubclassOptInRequired

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
fun interface FunctionalInterface { fun func() }

// enum classes and objects are not valid targets for @SubclassOptInRequired

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
enum class EnumKlass { ENTRY }

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
object Objekt

// sealed classifiers are not valid targets for @SubclassOptInRequired

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
sealed class SealedKlass

<!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
sealed interface SealedInterface

// local classes are not valid targets for @SubclassOptInRequired

fun foo() {
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    open class LocalOpenKlass
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    abstract class LocalAbstractKlass
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    class LocalKlass
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    data class LocalDataKlass(val arg: Int)
    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    object {}
}

class OuterKlass {

    // final classes are not valid targets for @SubclassOptInRequired

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    class NestedKlass

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    inner class InnerKlass

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    data class NestedDataKlass(val arg: Int)

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    @JvmInline value class NestedValueKlass(val arg: Int)

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    annotation class NestedAnnotationKlass

    // functional interface is not a valid target for @SubclassOptInRequired

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    fun interface NestedFunctionalInterface { fun func() }

    // enum classes and objects are not valid targets for @SubclassOptInRequired

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    enum class NestedEnumKlass { ENTRY }

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    object NestedObjekt

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    companion object

    // sealed classifiers are not valid targets for @SubclassOptInRequired

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    sealed class NestedSealedKlass

    <!SUBCLASS_OPT_IN_INAPPLICABLE!>@SubclassOptInRequired(ApiMarker::class)<!>
    sealed interface NestedSealedInterface
}

interface Interface

<!WRONG_ANNOTATION_TARGET!>@SubclassOptInRequired(ApiMarker::class)<!>
typealias TypeAlias = Interface

