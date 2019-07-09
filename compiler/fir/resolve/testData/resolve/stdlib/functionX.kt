import kotlin.jvm.functions.Function0

val x: Function0<Int> = { 42 }

val y: Function1<String, String> = { it }

class MyFunction : Function2<Int, String, Unit> {
    override fun invoke(p1: Int, p2: String) {}
}