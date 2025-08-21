package pack

interface MyInterface

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno(val value: Int)

open class SuperClass {
    companion object {
        const val CONST = 1
    }
}

class TopLevelClass(val i: MyInterface) : SuperClass(), MyInterface by @Anno(<expr>CONST</expr>) i {
    val CONST = "str"

    companion object {
        const val STR = "str"
    }
}
