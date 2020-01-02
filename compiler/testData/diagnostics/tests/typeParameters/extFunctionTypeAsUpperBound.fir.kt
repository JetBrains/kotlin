fun <T: Int.() -> String> foo() {}

val <T: Int.() -> String> bar = fun (x: Int): String { return x.toString() }

class A<T> where T : Double.(Int) -> Unit

interface B<T, U : T.() -> Unit>