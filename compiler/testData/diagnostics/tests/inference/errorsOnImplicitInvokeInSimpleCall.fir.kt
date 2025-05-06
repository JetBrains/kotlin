// RUN_PIPELINE_TILL: FRONTEND
inline operator fun <reified T> Int.invoke() = this

val a2 = <!CANNOT_INFER_PARAMETER_TYPE!>1()<!>
val a3 = 1.<!CANNOT_INFER_PARAMETER_TYPE!>invoke<!>()
