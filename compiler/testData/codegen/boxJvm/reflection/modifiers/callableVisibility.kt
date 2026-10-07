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
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

private fun privateTopLevelFun() = "privateTopLevelFun"
internal fun internalTopLevelFun() = "internalTopLevelFun"
@PublishedApi internal fun publishedApiFun() = "publishedApiFun"
private val privateTopLevelVal = "privateTopLevelVal"
internal val internalTopLevelVal = "internalTopLevelVal"

interface WithPrivateFun {
    private fun privateFun() = "WithPrivateFun.privateFun"
}

class WithPrivateFunImpl : WithPrivateFun

class Setters {
    var protectedSetter = "initial"
        protected set
    var internalSetter = "initial"
        internal set
}

object Obj {
    private fun privateFun() = "Obj.privateFun"
    internal fun internalFun() = "Obj.internalFun"
}

class WithCompanion {
    companion object {
        private fun privateFun() = "WithCompanion.privateFun"
        internal fun internalFun() = "WithCompanion.internalFun"
    }
}

open class Base {
    protected open fun protectedFun() = "Base.protectedFun"
    internal open fun internalFun() = "Base.internalFun"
}

class Widened : Base() {
    public override fun protectedFun() = "Widened.protectedFun"
}

class FakeOverrides : Base()

private fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }

// Private and protected callables are not accessible by default, and can only be called after `isAccessible = true`.
private fun checkCallRequiresAccess(expected: Any?, callable: KCallable<*>, vararg args: Any?) {
    assertFailsWith<IllegalCallableAccessException> { callable.call(*args) }
    callable.isAccessible = true
    assertEquals(expected, callable.call(*args))
}

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

    ::privateTopLevelFun.let {
        assertEquals(KVisibility.PRIVATE, it.visibility)
        checkCallRequiresAccess("privateTopLevelFun", it)
    }
    ::internalTopLevelFun.let {
        assertEquals(KVisibility.INTERNAL, it.visibility)
        assertEquals("internalTopLevelFun", it.call())
    }
    ::publishedApiFun.let {
        assertEquals(KVisibility.INTERNAL, it.visibility)
        assertEquals("publishedApiFun", it.call())
    }
    ::privateTopLevelVal.let {
        assertEquals(KVisibility.PRIVATE, it.visibility)
        checkCallRequiresAccess("privateTopLevelVal", it)
    }
    ::internalTopLevelVal.let {
        assertEquals(KVisibility.INTERNAL, it.visibility)
        assertEquals("internalTopLevelVal", it.call())
    }
    WithPrivateFun::class.declaredMemberFunctions.single().let {
        assertEquals(KVisibility.PRIVATE, it.visibility)
        checkCallRequiresAccess("WithPrivateFun.privateFun", it, WithPrivateFunImpl())
    }

    (Setters::class.member("protectedSetter") as KMutableProperty<*>).let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals(KVisibility.PROTECTED, it.setter.visibility)
        val instance = Setters()
        checkCallRequiresAccess(Unit, it.setter, instance, "protected")
        assertEquals("protected", it.getter.call(instance))
    }
    (Setters::class.member("internalSetter") as KMutableProperty<*>).let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals(KVisibility.INTERNAL, it.setter.visibility)
        val instance = Setters()
        it.setter.call(instance, "internal")
        assertEquals("internal", it.getter.call(instance))
    }

    checkCallRequiresAccess("Obj.privateFun", Obj::class.member("privateFun"), Obj)
    assertEquals("Obj.internalFun", Obj::class.member("internalFun").call(Obj))
    checkCallRequiresAccess("WithCompanion.privateFun", WithCompanion.Companion::class.member("privateFun"), WithCompanion.Companion)
    assertEquals("WithCompanion.internalFun", WithCompanion.Companion::class.member("internalFun").call(WithCompanion.Companion))

    Widened::class.member("protectedFun").let {
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("Widened.protectedFun", it.call(Widened()))
    }
    FakeOverrides::class.member("protectedFun").let {
        assertEquals(KVisibility.PROTECTED, it.visibility)
        checkCallRequiresAccess("Base.protectedFun", it, FakeOverrides())
    }
    FakeOverrides::class.member("internalFun").let {
        assertEquals(KVisibility.INTERNAL, it.visibility)
        assertEquals("Base.internalFun", it.call(FakeOverrides()))
    }

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
