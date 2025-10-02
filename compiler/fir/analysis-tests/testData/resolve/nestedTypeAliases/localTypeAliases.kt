// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LocalTypeAliases
// ISSUE: KT-79185

open class Generic<T>

fun <T> funWithGenericParam() {
    class LocalClass

    typealias TAbeforRHSdeclaration = <!UNRESOLVED_REFERENCE!>LocalTA<!> // Unresolved

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> typealias LocalTA = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>LocalClass<!> // Prohibited
    typealias LocalTAWithCapturingRHS = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>Generic<T><!> // Prohibited
    typealias LocalTAWithUnresolvedTypeArg = Generic<<!UNRESOLVED_REFERENCE!>K<!>> // Prohibited

    typealias Recursive = <!RECURSIVE_TYPEALIAS_EXPANSION!>Recursive<!>

    val localClass = <!CANNOT_INFER_PARAMETER_TYPE!>LocalTA<!>() // Check there is no a crash
    val generic = <!CANNOT_INFER_PARAMETER_TYPE!>LocalTAWithCapturingRHS<!>() // Check there is no a crash
    val genericWithUnresolvedTypeArg = <!CANNOT_INFER_PARAMETER_TYPE!>LocalTAWithUnresolvedTypeArg<!>() // Check there is no a crash

    fun <K> localFunWithGenericParam() {
        typealias LocalLocalTA1 = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>Generic<T><!> // Prohibited
        typealias LocalLocalTA2 = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>Generic<K><!> // Prohibited
        typealias LocalLocalTA3 = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>LocalClass<!> // Prohibited
    }
}

fun regularFun() {
    open class LocalClass {
        <!NESTED_CLASS_NOT_ALLOWED!>class LocalClass2<!>

        typealias LocalTA2 = LocalClass2 // Disallowed (NESTED_CLASS_NOT_ALLOWED)
        typealias LocalTAToGenericWithSubstitutedTypeParameter2 = Generic<String> // Allowed
        typealias LocalTAWithTypeParameter2<T> = Generic<T> // Allowed

        val localClass2 = LocalTA2()
        val genericString2 = LocalTAToGenericWithSubstitutedTypeParameter2()
        val generic2 = LocalTAWithTypeParameter2<String>()
    }

    typealias LocalTA = LocalClass // Allowed
    typealias LocalTAToGenericWithSubstitutedTypeParameter = Generic<String> // Allowed
    typealias LocalTAWithTypeParameter<T> = Generic<T> // Allowed

    val anonObject = object : LocalTA() {
    }

    val localClass = LocalTA()
    val genericString = LocalTAToGenericWithSubstitutedTypeParameter()
    val generic = LocalTAWithTypeParameter<String>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, localProperty, nullableType,
propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
