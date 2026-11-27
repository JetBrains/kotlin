// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, try-expression -> paragraph 7 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: finally block has no effect on the value returned by the try-expression
 */

fun box(): String {
    var a = 1
    val tryVal1 = try { 3 } catch (e: Exception) { 5 } finally { 100}
    val tryVal2 = try { 3; throw Exception() } catch (e: Exception) { 5 } finally { 100 }
    if (tryVal1 == 3 && tryVal2 == 5)
        return "OK"
    else return "NOK"
}