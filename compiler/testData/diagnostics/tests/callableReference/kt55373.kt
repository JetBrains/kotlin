class One
fun one() = Unit
class Two

val test: Two = <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>One<!><!>
val test2: Two = <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>one<!><!>
val test3: (String) -> One = <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>One<!><!>
