package second

fun (String.() -> Unit)?.helloFun1() {
}

fun Function0<Unit>.helloFun2() {
}

fun @extension Function1<String, Unit>.helloFun3() {
}

fun @extension Function1<Int, Unit>.helloFun4() {
}

fun Function1<String, Unit>.helloFun5() {
}

fun Any.helloAny() {
}
