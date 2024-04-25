// RENDER_DIAGNOSTICS_FULL_TEXT
class Base<T : List<CharSequence>>
typealias Alias<T> = Base<List<T>>
val a = <!UPPER_BOUND_VIOLATED!>Alias<Any>()<!> // Also should be error
