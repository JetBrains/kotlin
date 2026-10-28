// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75375
// FIR_DUMP
// RENDER_DIAGNOSTIC_ARGUMENTS

fun interface TestInterface {
    fun test()
}

class TestClass : TestInterface {
    override fun test() {}
}

inline fun <reified A> A.unusedReceiver() where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; unusedReceiver")!>AA<!> : TestInterface {
    A::class
}

fun issueUsage(t: TestClass) {
    t.<!CANNOT_INFER_PARAMETER_TYPE("AA")!>unusedReceiver<!>()
    t.<!CANNOT_INFER_PARAMETER_TYPE("AA")!>unusedReceiver<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; fun <reified A, AA : TestInterface> A.unusedReceiver(): Unit")!><TestClass><!>()
}

fun noTypeParameters(a: <!UNRESOLVED_REFERENCE("AA")!>AA<!>): <!UNRESOLVED_REFERENCE("AA")!>AA<!> <!SYNTAX(": Type constraints are not allowed when no type parameters declared")!>where AA : Number<!> {
    val b: <!UNRESOLVED_REFERENCE("AA")!>AA<!> = a
    return b
}

fun <T> withTypeParameter(a: AA, t: T): AA where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; withTypeParameter")!>AA<!> : Number {
    val b: AA = a
    return b
}

fun calls() {
    <!NO_VALUE_FOR_PARAMETER("a")!>noTypeParameters<!>()
    noTypeParameters(1)
    noTypeParameters<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("0; fun noTypeParameters(a: ??? (Unresolved qualified name: AA)): ??? (Unresolved qualified name: AA)")!><Int><!>(1)
    noTypeParameters<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("0; fun noTypeParameters(a: ??? (Unresolved qualified name: AA)): ??? (Unresolved qualified name: AA)")!><Int, String><!>(1)

    withTypeParameter(1, "")
    withTypeParameter<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; fun <T, AA : Number> withTypeParameter(a: AA, t: T): AA")!><String><!>(1, "")
    withTypeParameter<String, Int>(1, "")
}

fun <T> twoConstraints(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; twoConstraints")!>AA<!> : Number, AA : Comparable<AA> {}

fun <T> shadowsClass(t: T) where T : TestInterface, <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("TestInterface; shadowsClass")!>TestInterface<!> : Any {
    t.<!UNRESOLVED_REFERENCE("test;  on receiver of type 'TestInterface (of fun <T : TestInterface, TestInterface : Any> shadowsClass)'")!>test<!>()
}

class C <!SYNTAX(": Type constraints are not allowed when no type parameters declared")!>where AA : Any<!>

class D<T> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; D")!>AA<!> : Any {
    fun get(a: AA): AA = a
}

fun classUsages() {
    C()
    C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("0; constructor(): C")!><Int><!>()
    <!CANNOT_INFER_PARAMETER_TYPE("AA")!>D<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; constructor<T, AA : Any>(): D<T, AA>")!><Int><!>()
    D<Int, String>()
    <!CANNOT_INFER_PARAMETER_TYPE("AA")!>D<!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS("2; constructor<T, AA : Any>(): D<T, AA>")!><Int><!>().get(1)
}

val <T> T.prop: AA where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; prop")!>AA<!> : Number get() = null!!

fun propertyUsage() {
    1.<!CANNOT_INFER_PARAMETER_TYPE("AA")!>prop<!>
    "".<!CANNOT_INFER_PARAMETER_TYPE("AA")!>prop<!>
}

interface Base {
    fun <T> member(t: T)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED("Class 'Impl'; member:fun <T> member(t: T): Unit")!>class Impl<!> : Base {
    <!NOTHING_TO_OVERRIDE("member;  Potential signatures for overriding:fun <T> member(t: T): Unit")!>override<!> fun <T> member(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; member")!>AA<!> : Any {}
}

fun <T> conflictingBounds(t: T) where <!INCONSISTENT_TYPE_PARAMETER_BOUNDS("T; interface Comparable<in T> : Any; String, Int"), NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; conflictingBounds")!>AA<!> : <!FINAL_UPPER_BOUND("String")!>String<!>, AA : <!FINAL_UPPER_BOUND("Int"), ONLY_ONE_CLASS_BOUND_ALLOWED!>Int<!> {}

fun <T> arrayBound(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; arrayBound"), UPPER_BOUND_CANNOT_BE_ARRAY!>AA<!> : Array<Int> {}

fun <T> cyclicBounds(t: T) where <!CYCLIC_GENERIC_UPPER_BOUND("AA, BB"), NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; cyclicBounds")!>AA<!> : BB, <!CYCLIC_GENERIC_UPPER_BOUND("AA, BB"), NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("BB; cyclicBounds")!>BB<!> : AA {}

fun <T> underscore(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("_; underscore")!>_<!> : Any {}

fun localFunction() {
    fun <T> local(a: AA, t: T): AA where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; local")!>AA<!> : Number = a
    local(1, "")
}

class EmptyTypeParameters<<!SYNTAX(": Type parameter declaration expected")!><!>> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; EmptyTypeParameters")!>AA<!> : Any

class GenericThrowable<<!SYNTAX(": Type parameter declaration expected")!><!>> : Throwable() where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; GenericThrowable")!>AA<!> : Any

enum class GenericEnum<<!SYNTAX(": Type parameter declaration expected")!><!>> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; GenericEnum"), TYPE_PARAMETERS_IN_ENUM!>AA<!> : Any { ENTRY }

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, classReference, enumDeclaration, enumEntry, funInterface,
funWithExtensionReceiver, functionDeclaration, getter, inline, integerLiteral, interfaceDeclaration, localFunction,
localProperty, nullableType, override, propertyDeclaration, propertyWithExtensionReceiver, reified, stringLiteral,
typeConstraint, typeParameter */
