package second

fun (String.() -> Unit)?.helloFun1() {
}

fun Function0<Unit>.helloFun2() {
}

fun @ExtensionFunctionType Function1<String, Unit>.helloFun3() {
}

fun @ExtensionFunctionType Function1<Int, Unit>.helloFun4() {
}

fun Function1<String, Unit>.helloFun5() {
}

fun Any.helloAny() {
}

// ALLOW_AST_ACCESS
