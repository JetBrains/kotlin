package second

annotation class Anno(val str: String)
val constant = "const"

class MyC<caret>lass : LazySchemeProcessor<@Anno("super1 $constant") Int, @Anno("super2 $constant") Int>() {
    override fun isSchemeFile(name: CharSequence) = name != "str"
}

abstract class LazySchemeProcessor<SCHEME : @Anno("bound1 $constant") Number, MUTABLE_SCHEME : @Anno("bound2 $constant") SCHEME> {
    open fun isSchemeFile(name: CharSequence) = true

    @Anno("property $constant")
    @get:Anno("property $constant")
    @set:Anno("property $constant")
    @setparam:Anno("property $constant")
    var propertyWithAnnotations: SCHEME

    var property: SCHEME
}
