annotation class Anno(val str: String)
val constant = "const"

class MyClass {
    lateinit var d: IntermediateClass<Int>
    val prop = object<caret> : IntermediateClass<@Anno("object $constant") Int> by d {
        override fun isSchemeFile(name: CharSequence) = name != "str"
    }
}

interface IntermediateClass<SCHEME : @Anno("bound $constant") Number> : BaseClass<@Anno("super $constant") SCHEME, @Anno("super $constant") Int> {
}

interface BaseClass<SCHEME : @Anno("base bound $constant") Number, MUTABLE_SCHEME> {
    fun isSchemeFile(name: CharSequence = genericCall<CharSequence>()) = true
    fun anotherFunction(name: CharSequence = genericCall<CharSequence>()) = true

    @Anno("property $constant")
    @get:Anno("property $constant")
    @set:Anno("property $constant")
    @setparam:Anno("property $constant")
    var propertyWithAnnotations = genericCall<SCHEME>()

    var property: SCHEME
}

fun <T> genericCall(): T = null!!