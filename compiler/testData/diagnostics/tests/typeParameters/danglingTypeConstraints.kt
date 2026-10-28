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
    t.unusedReceiver()
    t.unusedReceiver<TestClass>()
}

fun noTypeParameters(a: <!UNRESOLVED_REFERENCE("AA")!>AA<!>): <!UNRESOLVED_REFERENCE("AA")!>AA<!> <!SYNTAX(": Type constraints are not allowed when no type parameters declared")!>where AA : Number<!> {
    val b: <!UNRESOLVED_REFERENCE("AA")!>AA<!> = a
    return b
}

fun <T> withTypeParameter(a: <!UNRESOLVED_REFERENCE("AA")!>AA<!>, t: T): <!UNRESOLVED_REFERENCE("AA")!>AA<!> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; withTypeParameter")!>AA<!> : Number {
    val b: <!UNRESOLVED_REFERENCE("AA")!>AA<!> = a
    return b
}

fun calls() {
    <!NO_VALUE_FOR_PARAMETER("a")!>noTypeParameters<!>()
    noTypeParameters(1)
    noTypeParameters<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("0; fun noTypeParameters(a: ??? (Unresolved qualified name: AA)): ??? (Unresolved qualified name: AA)")!><Int><!>(1)
    noTypeParameters<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("0; fun noTypeParameters(a: ??? (Unresolved qualified name: AA)): ??? (Unresolved qualified name: AA)")!><Int, String><!>(1)

    withTypeParameter(1, "")
    withTypeParameter<String>(1, "")
    withTypeParameter<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("1; fun <T> withTypeParameter(a: ??? (Unresolved qualified name: AA), t: T): ??? (Unresolved qualified name: AA)")!><String, Int><!>(1, "")
}

fun <T> twoConstraints(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; twoConstraints")!>AA<!> : Number, <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; twoConstraints")!>AA<!> : Comparable<AA> {}

fun <T> shadowsClass(t: T) where T : TestInterface, <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("TestInterface; shadowsClass")!>TestInterface<!> : Any {
    t.test()
}

class C <!SYNTAX(": Type constraints are not allowed when no type parameters declared")!>where AA : Any<!>

class D<T> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; D")!>AA<!> : Any {
    fun get(a: <!UNRESOLVED_REFERENCE("AA")!>AA<!>): <!UNRESOLVED_REFERENCE("AA")!>AA<!> = a
}

fun classUsages() {
    C()
    C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("0; constructor(): C")!><Int><!>()
    D<Int>()
    D<!WRONG_NUMBER_OF_TYPE_ARGUMENTS("1; constructor<T>(): D<T>")!><Int, String><!>()
    D<Int>().get(1)
}

val <T> T.prop: <!UNRESOLVED_REFERENCE("AA")!>AA<!> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; prop")!>AA<!> : Number get() = null!!

fun propertyUsage() {
    1.prop
    "".prop
}

interface Base {
    fun <T> member(t: T)
}

class Impl : Base {
    override fun <T> member(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; member")!>AA<!> : Any {}
}

fun <T> conflictingBounds(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; conflictingBounds")!>AA<!> : String, <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; conflictingBounds")!>AA<!> : Int {}

fun <T> arrayBound(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; arrayBound")!>AA<!> : Array<Int> {}

fun <T> cyclicBounds(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; cyclicBounds")!>AA<!> : BB, <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("BB; cyclicBounds")!>BB<!> : AA {}

fun <T> underscore(t: T) where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("_; underscore")!>_<!> : Any {}

fun localFunction() {
    fun <T> local(a: <!UNRESOLVED_REFERENCE("AA")!>AA<!>, t: T): <!UNRESOLVED_REFERENCE("AA")!>AA<!> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; local")!>AA<!> : Number = a
    local(1, "")
}

class EmptyTypeParameters<<!SYNTAX(": Type parameter declaration expected")!><!>> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; EmptyTypeParameters")!>AA<!> : Any

class GenericThrowable<<!SYNTAX(": Type parameter declaration expected")!><!>> : Throwable() where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; GenericThrowable")!>AA<!> : Any

enum class GenericEnum<<!SYNTAX(": Type parameter declaration expected")!><!>> where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER("AA; GenericEnum")!>AA<!> : Any { ENTRY }

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, classReference, enumDeclaration, enumEntry, funInterface,
funWithExtensionReceiver, functionDeclaration, getter, inline, integerLiteral, interfaceDeclaration, localFunction,
localProperty, nullableType, override, propertyDeclaration, propertyWithExtensionReceiver, reified, stringLiteral,
typeConstraint, typeParameter */
