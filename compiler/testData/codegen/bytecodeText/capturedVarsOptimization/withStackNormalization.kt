// IGNORE_BACKEND: JVM_IR
// TODO KT-36648 Captured variables not optimized in JVM_IR

fun add(x: Int, y: Int) = x + y

fun test() {
    var x = 0
    run {
        x += add(1, try { 1 } catch (e: Throwable) { 42 })
    }
}

// 0 NEW
// 0 GETFIELD
// 0 PUTFIELD
