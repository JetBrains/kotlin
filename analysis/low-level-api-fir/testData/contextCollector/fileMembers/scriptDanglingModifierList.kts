@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Anno(val value: Int)

const val CONSTANT = 1

class BeforeClass {
    val memberProperty: Int = 1

    fun memberFunction(): Int = memberProperty
}

fun beforeFunction(): Int {
    val local = 1
    return local + CONSTANT
}

val beforeProperty: Int = beforeFunction()

typealias BeforeAlias = BeforeClass

beforeFunction()

@Anno(<expr>CONSTANT</expr>)
