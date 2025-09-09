fun basic(): String = ""

@MustUseReturnValues
object Annotated {
    fun annotated(): String = ""
}

fun test() {
    basic()
    Annotated.annotated()
    val _ = basic()
}
