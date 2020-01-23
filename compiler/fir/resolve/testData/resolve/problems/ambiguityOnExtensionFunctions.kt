interface A

interface B : A

val A.name: String?
    get() = ""

val B?.name: String?
    get() = ""

fun test(b: B) {
    val id = b.<!AMBIGUITY!>name<!>
}
