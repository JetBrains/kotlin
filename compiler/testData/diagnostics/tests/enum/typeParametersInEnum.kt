// KT-5696 Prohibit type parameters for enum classes

package bug

public enum class Foo<!TYPE_PARAMETERS_IN_ENUM!><T><!> {
    <!NO_GENERICS_IN_SUPERTYPE_SPECIFIER!>A<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><!>()
}
