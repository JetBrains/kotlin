// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses

@file:Suppress("INLINE_CLASS_DEPRECATED")

inline class A1(val x: Int)

@JvmInline
value class A2(val x: Int)

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
inline class A3(val x: Int)

<!UNSUPPORTED_FEATURE("The feature \"full value classes\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-XXLanguage:+FullValueClasses', but note that no stability guarantees are provided.")!>value<!> class A4(val x: Int)


inline class B1(val x: Int, val y: Int)

@JvmInline
value class B2(val x: Int, val y: Int)

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
inline class B3(val x: Int, val y: Int)

<!UNSUPPORTED_FEATURE("The feature \"full value classes\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-XXLanguage:+FullValueClasses', but note that no stability guarantees are provided.")!>value<!> class B4(val x: Int, val y: Int)


inline class C1(val x: B2)

@JvmInline
value class C2(val x: B2)

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
inline class C3(val x: B2)

<!UNSUPPORTED_FEATURE("The feature \"full value classes\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-XXLanguage:+FullValueClasses', but note that no stability guarantees are provided.")!>value<!> class C4(val x: B2)

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral, value */
