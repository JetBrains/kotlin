// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79185

open class Generic<T>

fun <T> funWithGenericParam() {
    class LocalClass

    <!UNSUPPORTED!>typealias TAbeforRHSdeclaration = <!UNRESOLVED_REFERENCE!>LocalTA<!><!> // Unresolved

    <!UNSUPPORTED!><!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> typealias LocalTA = LocalClass<!> // Prohibited
    <!UNSUPPORTED!>typealias LocalTAWithCapturingRHS = Generic<T><!> // Prohibited
    <!UNSUPPORTED!>typealias LocalTAWithUnresolvedTypeArg = Generic<<!UNRESOLVED_REFERENCE!>K<!>><!> // Prohibited

    <!UNSUPPORTED!>typealias Recursive = <!RECURSIVE_TYPEALIAS_EXPANSION!>Recursive<!><!>

    val localClass = <!CANNOT_INFER_PARAMETER_TYPE!>LocalTA<!>() // Check there is no a crash
    val generic = <!CANNOT_INFER_PARAMETER_TYPE!>LocalTAWithCapturingRHS<!>() // Check there is no a crash
    val genericWithUnresolvedTypeArg = <!CANNOT_INFER_PARAMETER_TYPE!>LocalTAWithUnresolvedTypeArg<!>() // Check there is no a crash

    fun <K> localFunWithGenericParam() {
        <!UNSUPPORTED!>typealias LocalLocalTA1 = Generic<T><!> // Prohibited
        <!UNSUPPORTED!>typealias LocalLocalTA2 = Generic<K><!> // Prohibited
        <!UNSUPPORTED!>typealias LocalLocalTA3 = LocalClass<!> // Prohibited
    }
}

fun regularFun() {
    open class LocalClass {
        <!NESTED_CLASS_NOT_ALLOWED!>class LocalClass2<!>

        <!UNSUPPORTED!>typealias LocalTA2 = LocalClass2<!> // Disallowed (NESTED_CLASS_NOT_ALLOWED)
        <!UNSUPPORTED!>typealias LocalTAToGenericWithSubstitutedTypeParameter2 = Generic<String><!> // Allowed
        <!UNSUPPORTED!>typealias LocalTAWithTypeParameter2<T> = Generic<T><!> // Allowed

        val localClass2 = LocalTA2()
        val genericString2 = LocalTAToGenericWithSubstitutedTypeParameter2()
        val generic2 = LocalTAWithTypeParameter2<String>()
    }

    <!UNSUPPORTED!>typealias LocalTA = LocalClass<!> // Allowed
    <!UNSUPPORTED!>typealias LocalTAToGenericWithSubstitutedTypeParameter = Generic<String><!> // Allowed
    <!UNSUPPORTED!>typealias LocalTAWithTypeParameter<T> = Generic<T><!> // Allowed

    val anonObject = object : LocalTA() {
    }

    val localClass = LocalTA()
    val genericString = LocalTAToGenericWithSubstitutedTypeParameter()
    val generic = LocalTAWithTypeParameter<String>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, localProperty, nullableType,
propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
