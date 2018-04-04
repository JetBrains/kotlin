package runtime.memory.weak0

import kotlin.test.*
import konan.ref.*

data class Data(val s: String)

fun localWeak(): WeakReference<Data>  {
    val x = Data("Hello")
    val weak = WeakReference(x)

    println(weak.get())
    return weak
}

fun multiWeak(): Array<WeakReference<Data>>  {
    val x = Data("Hello")
    val weaks = Array(100, { WeakReference(x) } )
    weaks.forEach {
        it -> if (it.get()?.s != "Hello") throw Error("bad reference")
    }
    return weaks
}

@Test fun runTest() {
    val weak = localWeak()
    val value = weak.get()
    println(value?.toString())

    val weaks = multiWeak()
    weaks.forEach {
        it -> if (it.get()?.s != null) throw Error("not null")
    }
    println("OK")
}