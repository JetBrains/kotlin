// "Remove val/var from function, loop and catch parameters in project" "true"

class Class(a: Int, val b: Int, var c: Int, vararg val d: Int) {

}

fun f(a: Int, b: Int, c: Int, vararg d: Int) {
    for (i in d) {
    }
    for (i in d) {
    }

    try {
    } catch (e: Exception) {
    } catch (e: Exception) {
    } catch (e: Exception) {
    }
}