package prop.`in`.`companion`.indirect

class Test {
    fun test() {
        val x = <expr>indirectPointer</expr>
    }
    companion object {
        // effectively constant
        val someField = "something"
        val indirectPointer = someField
    }
}
