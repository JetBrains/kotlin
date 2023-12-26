// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
//KT-3190 Compiler crash if function called 'invoke' calls a closure
// IGNORE_BACKEND: JS
// JS backend does not allow to implement Function{N} interfaces
// JVM_ABI_K1_K2_DIFF: KT-63864

fun box(): String {
    val test = Cached<Int,Int>({ it + 2 })
    return if (test(1) == 3) "OK" else "fail"
}

class Cached<K, V>(private val generate: (K)->V): Function1<K, V> {
    val store = HashMap<K, V>()

    // Everything works just fine if 'invoke' method is renamed to, for example, 'get'
    override fun invoke(p1: K) = store.getOrPut(p1) { generate(p1) }
}

//from library
fun <K,V> MutableMap<K,V>.getOrPut(key: K, defaultValue: ()-> V) : V {
    if (this.containsKey(key)) {
        return this.get(key) as V
    } else {
        val answer = defaultValue()
        this.put(key, answer)
        return answer
    }
}
