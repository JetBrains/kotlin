// IGNORE_BACKEND: JVM_IR
// TODO KT-36648 Captured variables not optimized in JVM_IR

fun test() {
    var x = 0
    run { ++x }
}

// 0 NEW
// 0 GETFIELD
// 0 PUTFIELD
