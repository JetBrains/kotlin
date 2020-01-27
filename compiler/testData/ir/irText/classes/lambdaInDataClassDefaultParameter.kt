// IGNORE_BACKEND_FIR: ANY
data class A(val runA: A.(String) -> Unit = {})

data class B(val x: Any = object {})