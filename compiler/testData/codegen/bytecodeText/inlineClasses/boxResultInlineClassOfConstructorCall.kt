// !LANGUAGE: +InlineClasses

inline class Result<T>(val a: Any?)

fun test() {
    val a = Result<Int>(1) // valueOf
    val b = Result<String>("sample")

    val c = Result<Result<Int>>(a) // box
    val d = Result<Result<Int>>(Result<Int>(1)) // valueOf, box
}

// 2 INVOKESTATIC Result\$Erased.box
// 0 INVOKEVIRTUAL Result.unbox

// 2 valueOf
// 0 intValue
