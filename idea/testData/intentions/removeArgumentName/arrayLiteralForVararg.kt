annotation class AnnWithVararg(vararg val value: String, val s: String)

interface Result {
    @AnnWithVararg(<caret>value = ["foo", "bar"], s = "")
    val res2: Any
}
