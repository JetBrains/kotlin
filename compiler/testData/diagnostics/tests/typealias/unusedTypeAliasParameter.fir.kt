// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_TYPEALIAS_PARAMETER
typealias Test<T, X> = List<T>
typealias Test2<T, X> = Test<T, X>
