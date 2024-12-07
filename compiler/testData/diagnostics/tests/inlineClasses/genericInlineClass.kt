// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +InlineClasses, +GenericInlineClassParameter
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// SKIP_TXT

inline class ICAny<T>(val value: T)

inline class ICArray<T>(val value: Array<T>)

inline class ICList<T>(val value: List<T>)
