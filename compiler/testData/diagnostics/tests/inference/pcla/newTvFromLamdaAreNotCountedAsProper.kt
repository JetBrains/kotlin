// FIR_IDENTICAL
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

class Out<out X>

fun <T1> catching(body: suspend () -> T1): Out<T1> = TODO()
fun <R2, T2 : R2> Out<T2>.getOrElse2(onFailure: () -> R2): R2 = TODO()

fun <T3> myEmptyList(): List<T3> = TODO()

fun main(x: List<String>) {

    generate {
        val languages = catching { x }.getOrElse2 {
            myEmptyList()
        }

        yield(languages[0].length)
    }
}