annotation class Ann

val a: <!WRONG_ANNOTATION_TARGET!>@Ann<!> String? = ""
val b: (<!WRONG_ANNOTATION_TARGET!>@Ann<!> String)? = "" // false negative in K1, OK in K2

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

val c: @TypeAnn String? = ""
val d: (@TypeAnn String)? = ""
