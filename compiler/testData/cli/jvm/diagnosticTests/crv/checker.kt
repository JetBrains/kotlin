fun basic(): String = ""

@MustUseReturnValue
object Annotated {
    fun annotated(): String = ""
}

fun test() {
    basic()
    Annotated.annotated()
    val _ = basic()
}
