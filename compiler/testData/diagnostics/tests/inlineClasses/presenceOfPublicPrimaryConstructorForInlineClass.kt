// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

inline class ConstructorWithDefaultVisibility(val x: Int)
inline class PublicConstructor public constructor(val x: Int)
inline class InternalConstructor internal constructor(val x: Int)
inline class ProtectedConstructor protected constructor(val x: Int)
inline class PrivateConstructor private constructor(val x: Int)
