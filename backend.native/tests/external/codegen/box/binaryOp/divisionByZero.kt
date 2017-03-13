// IGNORE_BACKEND: JS
// reason - no ArithmeticException in JS
fun box(): String {
    val a1 = 0
    val a2 = try { 1 / 0 } catch(e: ArithmeticException) { 0 }
    val a3 = try { 1 / a1 } catch(e: ArithmeticException) { 0 }
    val a4 = try { 1 / a2 } catch(e: ArithmeticException) { 0 }
    val a5 = try { 2 * (1 / 0) } catch(e: ArithmeticException) { 0 }
    val a6 = try { 2 * 1 / 0 } catch(e: ArithmeticException) { 0 }

    try { val s1 = "${2 * (1 / 0) }" } catch(e: ArithmeticException) { }

    return "OK"
}