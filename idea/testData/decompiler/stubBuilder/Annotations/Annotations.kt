@a public class Annotations private @a constructor(private @property:a @param:a val c1: Int, @property:a @param:a val c2: Int) {

    protected @a fun f() {
    }

    private fun annotationWithVararg(@a vararg i: Int) {}

    @b(E.E1) private val c: Int = 1

    @a @b(E.E2) public fun g(@a p1: E) {
    }

    var withCustomAccessors: Int = 0
    //TODO: accessor modifiers are lost
        @a get
        @a private set


    private @b(E.E2) companion object {

    }

    class Nested @a private @b(E.E1) @b(E.E2) constructor()

    fun types(param: @a @b(E.E1) LongRange): @a @b(E.E2) Unit {}
}

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class a

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS,
        AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class b(val e: E)

enum class E { E1, E2 }