// IS_APPLICABLE: false

interface A
interface B

fun prepare(x: A) = x
fun use(x: A) {}

fun test(x: A) {
    val xx = <caret>if (x is B) x else prepare(x)
    use(xx)
}