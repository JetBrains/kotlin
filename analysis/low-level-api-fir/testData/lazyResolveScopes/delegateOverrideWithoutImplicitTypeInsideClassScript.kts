annotation class Anno(val str: String)
val constant = "const"

lateinit var d: IntermediateInterface
class MyC<caret>lass : IntermediateInterface by d {
    override fun isSchemeFile(name: CharSequence): Boolean = name != "str"
}

interface IntermediateInterface : BaseInterface {
}

interface BaseInterface {
    fun isSchemeFile(name: CharSequence): Boolean = true
    fun anotherFunction(name: CharSequence): Boolean = true

    @Anno("property $constant")
    @get:Anno("property $constant")
    @set:Anno("property $constant")
    @setparam:Anno("property $constant")
    var propertyWithAnnotations: Int

    var property: Int
}
