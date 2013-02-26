// "Remove val/var from function, loop and catch parameters in project" "true"

class Class(a: Int, val b: Int, var c: Int, vararg val d: Int) {

}

fun f(a: Int, <caret>val b: Int, var c: Int, vararg val d: Int) {
    for (val i in d) {
    }
    for (var i in d) {
    }

    try {
    } catch (val e: Exception) {
    } catch (var e: Exception) {
    } catch (e: Exception) {
    }
}