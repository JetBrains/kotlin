// WITH_STDLIB
fun stringTemplate(x: Int) = "x = $x"
fun multilineString() = """Hello"""
fun comparison(a: Int, b: Int) = a < b
fun equality(a: Any, b: Any) = a == b
fun identity(a: Any, b: Any) = a !== b
fun conjunction(a: Boolean, b: Boolean) = a && b
fun disjunction(a: Boolean, b: Boolean) = a || b
fun isCheck(a: Any) = a is String
fun notIsCheck(a: Any) = a !is String
fun throwing() = throw IllegalStateException()
fun parenthesized() = (1)
fun annotated() = @Suppress("UNUSED") 1
fun labeled() = label@ 1

fun unresolvedComparison() = unresolved < 1
fun unresolvedEquality() = unresolved == 1
fun unresolvedIsCheck(a: Any) = a is Unresolved
fun incompleteComparison(a: Int) = a <
fun whileLoop() = while (true) {}
fun doWhileLoop() = do {} while (true)
fun forLoop() = for (i in 1..2) {}
fun returning() = return
fun breaking() = break
fun continuing() = continue

val comparisonProperty = 1 < 2
val throwingGetter get() = throw IllegalStateException()

fun container() {
    val equalityLocal = 1 == 2
    val stringTemplateLocal = "${1}"
    fun throwingLocal() = throw IllegalStateException()
}
