import kotlin.properties.Delegates

val prop: Int by Delegates.lazy {
    print(1)
    val v = f()
    <caret>g()
    v + 1
}

fun f(): Int = 1
fun g(): Int = 2