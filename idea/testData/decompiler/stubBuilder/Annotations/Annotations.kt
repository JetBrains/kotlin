a public class Annotations private @a constructor(private @a val c1: Int, @a val c2: Int) {

    protected a fun f() {
    }

    private fun annotationWithVararg(a vararg i: Int) {}

    b(E.E1) private val c: Int = 1

    a b(E.E2) public fun g(@a p1: E) {
    }

    var withCustomAccessors: Int = 0
    //TODO: accessor modifiers are lost
        @a get
        @a private set


    private b(E.E2) companion object {

    }

    class Nested @a private @b(E.E1) @b(E.E2) constructor()

    fun types(param: @a @b(E.E1) DoubleRange): @a @b(E.E2) Unit {}
}

target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION,
       AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
       AnnotationTarget.TYPE, AnnotationTarget.CLASSIFIER)
annotation class a

target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASSIFIER,
       AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
annotation(repeatable = true, retention = AnnotationRetention.SOURCE) class b(val e: E)

enum class E { E1 E2 }