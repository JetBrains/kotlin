annotation class AnnWithArray(val value: Array<String>)

interface Result {
    @AnnWithArray(<caret>value = ["foo", "bar"])
    val res1: Any
}
