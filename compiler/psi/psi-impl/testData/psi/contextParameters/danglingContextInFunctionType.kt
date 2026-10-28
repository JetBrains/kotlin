// COMPILATION_ERRORS

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

interface Properties {
    val ctx1: context(Int)
    val ctx2: context(Int) ()
    val ctx3: context(Int) () -
    val ctx4: context(Int) Int
    val ctx4Nullable: context(Int) String?
    val ctx5: context(Int) Int.
    val ctx5Parenthesized: context(Int) (String).
    val named: context(a: Int)
    val severalEntries: context(Int, String)
    val annotated: context(@Anno("1" + "2") Int)
    val suspendModifier: suspend context(Int)
    val typeArgument: List<context(Int)>
}

fun returnType(): context(Int) = TODO()

fun valueParameter(p: context(Int) String.) {}

fun <T> intersection(p: context(Int) T & Any) {}

fun body() {
    val local: context(Int) = TODO()
    val localReceiver: context(Int) Int. = TODO()
}
