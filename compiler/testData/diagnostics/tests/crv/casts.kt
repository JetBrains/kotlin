// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun getAny(): Any = ""
fun Any.consume(): Unit = Unit

fun intF(): Int = 42

val nonLocal: Any get() = ""

fun consume(s: String) {}

fun locals(a: Any) {
    a
    a as String
    val b: Any = ""
    b
    b as String
}

fun nonLocals() {
    nonLocal
    nonLocal as String
    val used = nonLocal as String
    consume(nonLocal as String)
    (nonLocal as String).consume()
}

fun classRefs(instance: Any) {
    instance::class
    String::class
    nonLocal::class
    val s = String::class
    val ss = instance::class
    val sss = nonLocal::class
}

fun whenInstance() {
    val e = when (nonLocal) {
        is String -> "a"
        is Boolean -> "b"
        else -> "c"
    }

    when (nonLocal) {
        is String -> intF()
        is Boolean -> getAny()
        else -> nonLocal
    }
}

fun castsInIf() {
    val x = if (intF() > 10) getAny() as String else ""
    val y = if (intF() > 10) {
        getAny() // unused
        getAny() as String // used
    } else {
        ""
    }
}
