@a public class Annotations private @a constructor(private @property:a @param:a val c1: Int, @property:a @param:a val c2: Int) {
    @a() val hasValueArguments = 42

    protected @a fun f() {
    }

    inline fun <reified @a T, @a F> inlineFun() { }

    private fun annotationWithVararg(@a vararg i: Int) {}

    @b(E.E1) private val c: Int = 1

    @a @b(E.E2) public fun g(@a p1: E) {
    }

    var withCustomAccessors: Int = 0
    //TODO: accessor modifiers are lost
        @a get
        @a private set


    private @b(E.E2) companion object {
        @f val field = 42
    }

    class Nested @a private @b(E.E1) @b(E.E2) constructor()

    enum class En { Entry1, @a @b(E.E2) Entry2, @a @c Entry3 }

    fun types(param: @a @b(E.E1) LongRange): @a @b(E.E2) Unit {}

    fun @receiver:a Int.foo() {}
    val @receiver:a Int.receiverField: String? get() = null

    @get: a
    val getterField : String? = null

    @set: a
    var setterField : String? = null

    @field: a
    val ownField : String? = null

    @setparam:a
    var setParam: String? = null

    @delegate:a
    val deleage by lazy { 12 }

    class ClassWithConstructor(@param: a val b: Int)
}

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.FIELD)
annotation class a

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS,
        AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class b(val e: E)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class c

@Target(AnnotationTarget.FIELD)
annotation class f

enum class E { E1, E2 }
