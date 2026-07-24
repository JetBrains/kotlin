// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: -FullValueClasses

<!UNSUPPORTED_FEATURE!>value<!> class Final constructor(val value1: String, val value2: String)
<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Final2 constructor(val value1: String)
value class Final4 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>constructor()<!>
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class Final6

@JvmInline
value class Final8 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>constructor(val value1: String, val value2: String)<!>
@JvmInline
value class Final10 constructor(val value1: String)
@JvmInline
value class Final12 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>constructor()<!>
@JvmInline
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class Final14

<!INLINE_CLASS_DEPRECATED!>inline<!> class Final16 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>constructor(val value1: String, val value2: String)<!>
<!INLINE_CLASS_DEPRECATED!>inline<!> class Final18 constructor(val value1: String)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Final20 <!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>constructor()<!>
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, INLINE_CLASS_DEPRECATED!>inline<!> class Final22

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
