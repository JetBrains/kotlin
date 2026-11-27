// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: If there are several catch blocks which match the exception type, the first one is picked.
 */
fun throwExceptionB(b: Boolean) = run { if (b) throw ExcB() }


class ExcA() : Exception()
class ExcB() : Exception()

fun box(): String {

    var flag = false
    try {
        throwExceptionB(true)
        flag = true
    } catch (e: ExcA) {
        return "NOK"
    } catch (e: Exception) {
        return if (flag)
            "NOK"
        else "OK"
    }catch (e : ExcB){
        return "NOK"
    }
    return "NOK"
}