// RENDER_DIAGNOSTICS_FULL_TEXT
class Base<T : List<CharSequence>>
typealias Alias<T> = Base<List<T>>
val a = Alias<<!UPPER_BOUND_VIOLATED_WARNING("List<CharSequence>; List<Any>")!>Any<!>>() // Also should be error
