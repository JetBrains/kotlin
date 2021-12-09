// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses, +GenericInlineClassParameter
// SKIP_TXT

inline class ICAny<T>(val value: T)

inline class ICArray<T>(val value: Array<T>)

inline class ICList<T>(val value: List<T>)
