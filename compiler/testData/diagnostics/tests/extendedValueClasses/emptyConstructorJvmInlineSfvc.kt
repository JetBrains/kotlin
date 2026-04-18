// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB


@JvmInline
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS("@JvmInline value")!>value<!> class FinalA1

@JvmInline
value class FinalB1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>

@JvmInline
value class FinalC1(val x: Int)

@JvmInline
value class FinalD1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>

<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS("final value")!>value<!> class FinalA2

value class FinalB2<!VALUE_CLASS_EMPTY_CONSTRUCTOR("Final value")!>()<!>

value class FinalC2(val x: Int)

value class FinalD2(val x: Int, val y: Int)


@JvmInline
<!VALUE_CLASS_NOT_FINAL!>open<!> <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS("@JvmInline value")!>value<!> class OpenA1

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>open<!> value class OpenB1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>open<!> value class OpenC1(val x: Int)

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>open<!> value class OpenD1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>

<!VALUE_CLASS_OPEN!>open<!> value class OpenA2

<!VALUE_CLASS_OPEN!>open<!> value class OpenB2()

<!VALUE_CLASS_OPEN!>open<!> value class OpenC2(val x: Int)

<!VALUE_CLASS_OPEN!>open<!> value class OpenD2(val x: Int, val y: Int)


@JvmInline
<!VALUE_CLASS_NOT_FINAL!>abstract<!> <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS("@JvmInline value")!>value<!> class AbstractA1

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>abstract<!> value class AbstractB1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>abstract<!> value class AbstractC1(val x: Int)

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>abstract<!> value class AbstractD1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>

abstract value class AbstractA2

abstract value class AbstractB2()

abstract value class AbstractC2(<!ABSTRACT_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val x: Int<!>)

abstract value class AbstractD2(<!ABSTRACT_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val x: Int<!>, <!ABSTRACT_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val y: Int<!>)


@JvmInline
<!VALUE_CLASS_NOT_FINAL!>sealed<!> <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS("@JvmInline value")!>value<!> class SealedA1

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>sealed<!> value class SealedB1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>sealed<!> value class SealedC1(val x: Int)

@JvmInline
<!VALUE_CLASS_NOT_FINAL!>sealed<!> value class SealedD1<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>

sealed value class SealedA2

sealed value class SealedB2()

sealed value class SealedC2(<!SEALED_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val x: Int<!>)

sealed value class SealedD2(<!SEALED_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val x: Int<!>, <!SEALED_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val y: Int<!>)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, sealed, value */
