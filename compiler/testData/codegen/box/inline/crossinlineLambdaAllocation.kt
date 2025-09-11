// ISSUE: KT-69497
// IGNORE_BACKEND: JVM_IR
// ^^^ KT-75642
// NO_COMMON_FILES
// DUMP_IR
// DUMP_IR_AFTER_INLINE
// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS

class Pair<T>(val first: T, val second: T)

var globalCounter = 0

inline fun <T> runTwice(f: () -> T) = Pair(f(), f())

inline fun bar(crossinline test: () -> Int): Int {
    var outsideCounter = 0
    val x = runTwice {
        var insideCounter = 0
        val r = object {
            fun count() {
                globalCounter++
                outsideCounter++
                insideCounter++
            }
            fun foo() = test()
        }
        r.count()
        outsideCounter += insideCounter
        r
    }
    globalCounter += outsideCounter
    return x.first.foo() + x.second.foo()
}

fun box(): String {
    val result = bar { 5 }
    if (result != 5 + 5) return "result = $result"
    if (globalCounter != 6) return "globalCounter = $globalCounter"
    return "OK"
}