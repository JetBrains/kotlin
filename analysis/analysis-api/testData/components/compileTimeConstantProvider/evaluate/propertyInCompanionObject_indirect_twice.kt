package prop.`in`.`companion`.indirect.twice

class Test {
    fun test() {
        val x = <expr>oneMore</expr>
    }
    companion object {
        // effectively constant
        val someField = "something"
        val indirectPointer = someField
        val oneMore = indirectPointer
    }
}
