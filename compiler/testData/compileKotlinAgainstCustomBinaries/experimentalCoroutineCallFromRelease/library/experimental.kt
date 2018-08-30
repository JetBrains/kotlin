val c: suspend () -> Unit = {}

class WithTypeParameter<T: suspend() -> Unit> {}

fun returnsSuspend() : suspend() -> Unit = {}

fun <T: suspend () -> Unit> withTypeParameter() = {}

fun suspendFunctionNested(x: List<suspend () -> Unit>) {}
fun suspendFunctionNestedInFunctionType(x: (suspend () -> Unit) -> Unit) {}

fun suspendFunctionType3(x: suspend (Int, Int, Int) -> Unit) {}

fun suspendVarargs(vararg x: suspend () -> Unit) {}
