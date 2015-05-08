// KT-5696 Prohibit type parameters for enum classes

package bug

public enum class Foo<!TYPE_PARAMETERS_IN_ENUM!><T><!> {
    A : <!ENUM_ENTRY_USES_DEPRECATED_SUPER_CONSTRUCTOR!>Foo<String>()<!>
}
