// !LANGUAGE: +InlineClasses

inline class Result<T>(val a: Any?)

fun test() {
    val a = Result<Int>(1) // valueOf
    val b = Result<String>("sample")

    val c = Result<Result<Int>>(a)
    val d = Result<Result<Int>>(Result<Int>(1)) // valueOf
}

// 0 INVOKESTATIC Result\$Erased.box
// 0 INVOKEVIRTUAL Result.unbox

// 2 valueOf
// 0 intValue
