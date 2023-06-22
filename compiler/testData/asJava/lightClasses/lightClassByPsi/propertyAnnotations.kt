// !GENERATE_PROPERTY_ANNOTATIONS_METHODS

annotation class Anno(val p: String = "")


@Deprecated("deprecated")
val deprecated = 0

@Volatile
@Transient
var jvmFlags = 0

class C {
    companion object {
        @Anno("x")
        val x = 1

        @JvmStatic
        @Anno("y")
        val y = 2
    }
}

@Anno("property")
val <T: Any> @receiver:Anno("receiver") T.extensionProperty1: Int
    get() = 0

@Anno("property")
val <T: Any> @receiver:Anno("receiver") List<T>.extensionProperty2: Int
    get() = 0

@Anno("property")
val <X, Y: List<X>, Z: Map<X, Y>> @receiver:Anno("receiver") Z.extensionProperty3: Int
    get() = 0

@Anno("nullable")
val nullable: String? = null

@Anno("nonNullable")
val nonNullable: String = ""

open class O {
    @Anno("private")
    private val privateProperty: Int get() = 1

    @Anno("protected")
    protected val protectedProperty = 1
}