// RUN_PIPELINE_TILL: SOURCE
inline operator fun <reified T> Int.invoke() = this

val a2 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>1<!>()
val a3 = 1.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>invoke<!>()
