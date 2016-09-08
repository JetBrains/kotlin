
class WithFields(var i: Int) {

    var j: Int

    init {
        this.j = this.i
    }
}

fun test_simple_field(): Int {
    val i = WithFields(1)
    return i.j
}

fun test_field_assignment(i: Int): Int {
    val k = WithFields(i)
    return k.j
}
