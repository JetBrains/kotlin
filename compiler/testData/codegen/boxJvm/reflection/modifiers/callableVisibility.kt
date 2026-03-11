// TARGET_BACKEND: JVM
// JAVAC_OPTIONS: -parameters
// WITH_REFLECT
// FILE: JavaConstructors.java
public class JavaConstructors {
    public JavaConstructors(int public_) {}
    protected JavaConstructors(String protected_) {}
    /* package-private */ JavaConstructors(long package_private) {}
    private JavaConstructors(double private_) {}
}

// FILE: box.kt
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.test.assertEquals

open class Foo<in T> {
    public fun publicFun() {}
    protected fun protectedFun() {}
    internal fun internalFun() {}
    private fun privateFun() {}
    private fun privateToThisFun(): T = null!!

    fun getProtectedFun() = this::protectedFun
    fun getPrivateFun() = this::privateFun
    fun getPrivateToThisFun(): KFunction<*> = this::privateToThisFun

    public val publicVal = Unit
    protected val protectedVar = Unit
    internal val internalVal = Unit
    private val privateVal = Unit
    private val privateToThisVal: T? = null

    fun getProtectedVar() = this::protectedVar
    fun getPrivateVal() = this::privateVal
    fun getPrivateToThisVal(): KProperty<*> = this::privateToThisVal

    public var publicVarPrivateSetter = Unit
        private set

    fun getPublicVarPrivateSetter() = this::publicVarPrivateSetter
}

public var publicVarPrivateSetter = Unit
    private set

class Constructors {
    public constructor(public: Int)
    protected constructor(protected: String)
    internal constructor(internal: Long)
    private constructor(private: Double)
}

fun box(): String {
    val f = Foo<String>()

    assertEquals(KVisibility.PUBLIC, f::publicFun.visibility)
    assertEquals(KVisibility.PROTECTED, f.getProtectedFun().visibility)
    assertEquals(KVisibility.INTERNAL, f::internalFun.visibility)
    assertEquals(KVisibility.PRIVATE, f.getPrivateFun().visibility)
    assertEquals(KVisibility.PRIVATE, f.getPrivateToThisFun().visibility)

    assertEquals(KVisibility.PUBLIC, f::publicVal.visibility)
    assertEquals(KVisibility.PROTECTED, f.getProtectedVar().visibility)
    assertEquals(KVisibility.INTERNAL, f::internalVal.visibility)
    assertEquals(KVisibility.PRIVATE, f.getPrivateVal().visibility)
    assertEquals(KVisibility.PRIVATE, f.getPrivateToThisVal().visibility)

    assertEquals(KVisibility.PUBLIC, f.getPublicVarPrivateSetter().visibility)
    assertEquals(KVisibility.PUBLIC, f.getPublicVarPrivateSetter().getter.visibility)
    assertEquals(KVisibility.PRIVATE, f.getPublicVarPrivateSetter().setter.visibility)

    assertEquals(KVisibility.PUBLIC, ::publicVarPrivateSetter.visibility)
    assertEquals(KVisibility.PUBLIC, ::publicVarPrivateSetter.getter.visibility)
    assertEquals(KVisibility.PRIVATE, ::publicVarPrivateSetter.setter.visibility)

    fun KClass<*>.ctor(visibility: String) = constructors.single { it.parameters.single().name == visibility }

    assertEquals(KVisibility.PUBLIC, Constructors::class.ctor("public").visibility)
    assertEquals(KVisibility.PROTECTED, Constructors::class.ctor("protected").visibility)
    assertEquals(KVisibility.INTERNAL, Constructors::class.ctor("internal").visibility)
    assertEquals(KVisibility.PRIVATE, Constructors::class.ctor("private").visibility)

    assertEquals(KVisibility.PUBLIC, JavaConstructors::class.ctor("public_").visibility)
    // Java's protected also allows access in the same package, so it's not the same as Kotlin's protected.
    assertEquals(null, JavaConstructors::class.ctor("protected_").visibility)
    assertEquals(null, JavaConstructors::class.ctor("package_private").visibility)
    assertEquals(KVisibility.PRIVATE, JavaConstructors::class.ctor("private_").visibility)

    return "OK"
}
