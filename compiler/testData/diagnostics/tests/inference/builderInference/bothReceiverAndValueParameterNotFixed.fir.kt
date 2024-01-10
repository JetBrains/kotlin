// ISSUE: KT-64876
class Controller<K>

fun <T1> generate1(lambda: T1.(Controller<T1>) -> Unit): T1 = TODO()
fun <T2> generate2(lambda: Controller<T2>.(T2) -> Unit): T2 = TODO()

fun consume(f: Controller<String>) {}
fun consumeString(f: String) {}

fun main() {
    // We don't expect to allow running PCLA with not fixed TV as receiver
    // because all the calls should be resolved inside its member scope and we can't do that properly
    generate1 <!CANNOT_INFER_PARAMETER_TYPE!>{
        consume(it)
    }<!>.length

    generate2 {
        consume(this)
    }.length

    generate2 {
        consumeString(it)
    }.length
}
