// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-33247
// WITH_STDLIB

// KT-33247: Bad performance when editing Kotlin code with many overloaded function calls and complex type inference

fun process(x: Int): Int = x * 2
fun process(x: Long): Long = x * 2
fun process(x: Double): Double = x * 2
fun process(x: String): String = x + x
fun process(x: List<Int>): List<Int> = x + x
fun process(x: Any): Any = x

fun test() {
    val a = process(1)
    val b = process(1L)
    val c = process(1.0)
    val d = process("hello")
    val e = process(listOf(1, 2, 3))
    val f = process(a)

    val result = listOf(a, b, c, d, e, f).map { process(it) }
    println(result)
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
multiplicativeExpression, propertyDeclaration, stringLiteral */
