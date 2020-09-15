// PROBLEM: none
interface I {
    val v: String?
}

class C : I {
    override val v: String?<caret>
        get() = ""
}