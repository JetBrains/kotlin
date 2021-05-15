// FIR_COMPARISON
annotation class Anno
typealias TypedAnno = Anno


@Ty<caret>
fun usage() {

}

// INVOCATION_COUNT: 0
// EXIST: TypedAnno

