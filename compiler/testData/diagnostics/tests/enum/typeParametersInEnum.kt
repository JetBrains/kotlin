// RUN_PIPELINE_TILL: FRONTEND
// KT-5696 Prohibit type parameters for enum classes

package bug

public enum class Foo<!TYPE_PARAMETERS_IN_ENUM!><T><!> {
    A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><!>()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, nullableType, typeParameter */
