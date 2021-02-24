// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Bound1
interface Bound2
object First : Bound1, Bound2
object Second : Bound1, Bound2

interface WithParam1<out T>
interface WithParam2<out T>
class ClsWithParam1<out T> : WithParam1<T>, WithParam2<T>
class ClsWithParam2<out T> : WithParam1<T>, WithParam2<T>

fun <S : Bound1> intersect(vararg elements: S): S = TODO()
fun <T: Bound1, P : WithParam1<T>> combineParams(first: T, vararg args: P): P = TODO()

fun topLevel() = combineParams(
    intersect(First, Second),
    ClsWithParam1<First>(),
    ClsWithParam2<Second>()
)

fun test() {
    topLevel()
}
