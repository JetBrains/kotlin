class One
fun one() = Unit
class Two

val test: Two = <!INITIALIZER_TYPE_MISMATCH!>::One<!>
val test2: Two = <!INITIALIZER_TYPE_MISMATCH!>::one<!>
val test3: (String) -> One = <!INITIALIZER_TYPE_MISMATCH!>::One<!>
