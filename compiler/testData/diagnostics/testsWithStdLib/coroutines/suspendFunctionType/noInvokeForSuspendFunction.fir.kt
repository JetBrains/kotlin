fun test1(sfn: suspend () -> Unit) = sfn()
fun test2(sfn: suspend () -> Unit) = sfn.invoke()
