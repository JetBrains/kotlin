// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LocalTypeAliases
// SKIP_FIR_DUMP

open class Generic<T>

class TAOwner {
    typealias DirectRecursiveTA = <!RECURSIVE_TYPEALIAS_EXPANSION!>DirectRecursiveTA<!>
    typealias IndirectRecursiveTA = <!RECURSIVE_TYPEALIAS_EXPANSION!>List<IndirectRecursiveTA><!>
    typealias IndirectRecursiveTA2 = <!RECURSIVE_TYPEALIAS_EXPANSION!>IndirectRecursiveTA3<!>
    typealias IndirectRecursiveTA3 = <!RECURSIVE_TYPEALIAS_EXPANSION!>IndirectRecursiveTA2<!>

    typealias TypeParamVariance<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in<!> T1, <!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> T2> = List<T1>

    typealias Bounds<T1: <!BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>> = List<T1>

    typealias FunTA = <!RECURSIVE_TYPEALIAS_EXPANSION!>(Int) -> RecursiveFunTA<!>
    typealias RecursiveFunTA = <!RECURSIVE_TYPEALIAS_EXPANSION!>(FunTA) -> Int<!>

    var prop: String = ""
        get() {
            typealias LocalInGet = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>
            return field
        }

    fun testLocalTA(): Unit {
        typealias LocalInMethod = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>
        class Local {
            typealias LocalInLocalClass = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>
        }
    }

    init {
        typealias LocalInInit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>
    }

    class <!REDECLARATION!>Nested<!>
    typealias <!REDECLARATION!>Nested<!> = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>

    inner class <!REDECLARATION!>Inner<!>
    typealias <!REDECLARATION!>Inner<!> = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>

    typealias <!REDECLARATION!>Conflicting<!> = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>

    val <!REDECLARATION!>Conflicting<!>: String = ""

    <!WRONG_MODIFIER_TARGET!>override<!> typealias OverrideTA = String
    <!WRONG_MODIFIER_TARGET!>lateinit<!> typealias LateinitTA = String
    <!WRONG_MODIFIER_TARGET!>abstract<!> typealias AbstractTa = String
    <!WRONG_MODIFIER_TARGET!>final<!> typealias FinalTA = String
    <!WRONG_MODIFIER_TARGET!>open<!> typealias OpenTA = String
}

class RecursiveInheritance : <!CYCLIC_INHERITANCE_HIERARCHY!>RecursiveInheritance.TA<!> {
    typealias TA = <!RECURSIVE_TYPEALIAS_EXPANSION!>RecursiveInheritance<!>
}

class CapturingTAOwner<T> {
    typealias Capturing = List<<!UNRESOLVED_REFERENCE!>T<!>>
    typealias Capturing2<T2> = Map<<!UNRESOLVED_REFERENCE!>T<!>, T2>
    inner class Inner<T>
    typealias Capturing3 = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>Inner<Int><!>

    typealias FunTA = (Int, List<<!UNRESOLVED_REFERENCE!>T<!>>) -> Unit
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, getter, in, init, inner, localClass,
nestedClass, nullableType, out, propertyDeclaration, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
