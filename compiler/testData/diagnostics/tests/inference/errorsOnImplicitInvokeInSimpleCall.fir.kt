inline operator fun <reified T> Int.invoke() = this

val a2 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>1()<!>
val a3 = 1.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>invoke<!>()
