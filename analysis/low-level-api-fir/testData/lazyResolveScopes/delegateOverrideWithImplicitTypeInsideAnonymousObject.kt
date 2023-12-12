annotation class Anno(val str: String)
val constant = "const"

class MyClass {
    lateinit var d: IntermediateInterface
    val prop = object<caret> : IntermediateInterface by d {
        override fun isSchemeFile(name: CharSequence) = name != "str"
    }
}

interface IntermediateInterface : BaseInterface {
}

interface BaseInterface {
    fun isSchemeFile(name: CharSequence = genericCall<CharSequence>()) = true
    fun anotherFunction(name: CharSequence = genericCall<CharSequence>()) = true

    @Anno("property $constant")
    @get:Anno("property $constant")
    @set:Anno("property $constant")
    @setparam:Anno("property $constant")
    var propertyWithAnnotations: Int

    var property: Int
}

fun <T> genericCall(): T = null!!
