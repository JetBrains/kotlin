// PROBLEM: none
interface I {
    fun f(): String?
}

class C : I {
    override fun f(): String?<caret> {
        return ""
    }
}