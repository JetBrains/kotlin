package prop.`in`.`companion`

class Test {
    fun test() {
        val x = <expr>someField</expr>
    }
    companion object {
        // effectively constant
        val someField = "something"
    }
}
