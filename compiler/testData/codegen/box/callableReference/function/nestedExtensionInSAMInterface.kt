class A(val a : String)

fun interface SamInterface {
    fun A.accept(i: A.(A.()-> String)-> String): String
}

fun A.bar(a: A.(A.()->String) -> String): String { return a.invoke(this) { this.a } }

fun box(): String {
    val a = SamInterface(A::bar)
    with(a) {
        return A("FAIL").accept {"OK"}
    }
}
