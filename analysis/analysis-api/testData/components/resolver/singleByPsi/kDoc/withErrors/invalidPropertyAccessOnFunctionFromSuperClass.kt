open class SuperClass {
    fun toName(): String = ""
}

class A : SuperClass()

/**
 * [A.<caret>toName.length]
 */
fun foo() {}
