// SKIP_TXT
// ISSUE: KT-55691

fun <T> materialize(): T = TODO()
fun <S> select(x: S, y: S): S = x

fun main() {
    // materialize's type argument is inferred to `Nothing?` in K1 and `String?` in K2
    val x: String? = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>materialize()<!>, null)

    // materialize's type argument is inferred to `Nothing?` both in K1 and K2
    select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>materialize()<!>, null)
}
