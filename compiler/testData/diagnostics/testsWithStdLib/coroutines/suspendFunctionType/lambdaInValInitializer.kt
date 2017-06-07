typealias SuspendFn = suspend () -> Unit

val test1: suspend () -> Unit = {}
val test2: suspend Any.() -> Unit = {}
val test3: suspend Any.(Int) -> Int = { k: Int -> k + 1 }
val test4: SuspendFn = {}

val test1f: suspend () -> Unit = fun () {}
val test2f: suspend Any.() -> Unit = fun Any.() {}
val test3f: suspend Any.(Int) -> Int = fun (k: Int) = k + 1
val test4f: SuspendFn = fun Any.() {}
