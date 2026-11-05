// RUN_PIPELINE_TILL: CODEGEN

@file:Suppress("INLINE_CLASS_DEPRECATED", <!ERROR_SUPPRESSION!>"INLINE_CLASS_DEPRECATED_ERROR"<!>)

inline class ConstructorWithDefaultVisibility(val x: Int)
inline class PublicConstructor public constructor(val x: Int)
inline class InternalConstructor internal constructor(val x: Int)
inline class ProtectedConstructor protected constructor(val x: Int)
inline class PrivateConstructor private constructor(val x: Int)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration */
