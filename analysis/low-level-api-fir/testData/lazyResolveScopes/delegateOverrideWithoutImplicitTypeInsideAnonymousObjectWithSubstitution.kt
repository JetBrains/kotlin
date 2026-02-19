annotation class Anno(val str: String)
val constant = "const"

class MyClass {
    lateinit var d: IntermediateClass<Int>
    val prop = objec<caret>t : IntermediateClass<@Anno("object $constant") Int> by d {
        override fun isSchemeFile(name: CharSequence): Boolean = name != "str"
    }
}

interface IntermediateClass<SCHEME : @Anno("bound $constant") Number> : BaseClass<@Anno("super $constant") SCHEME, @Anno("super $constant") Int> {
}

interface BaseClass<SCHEME : @Anno("base bound $constant") Number, MUTABLE_SCHEME> {
    fun isSchemeFile(name: CharSequence): Boolean = true
    fun anotherFunction(name: SCHEME = genericCall<SCHEME>()): Boolean = true

    @Anno("property $constant")
    @get:Anno("property $constant")
    @set:Anno("property $constant")
    @setparam:Anno("property $constant")
    var propertyWithAnnotations: SCHEME

    var property: SCHEME
}

fun <T> genericCall(): T = null!!
