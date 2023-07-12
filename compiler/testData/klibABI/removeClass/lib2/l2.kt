@file:Suppress("NOTHING_TO_INLINE")

fun createRemovedClass() {
    check(RemovedClass().toString() != "Yellow Submarine")
}

interface Interface<T> {
    val value: T
}

class InterfaceImplParameterizedByClass : Interface<Class> {
    override val value: Class = Class()
}

class InterfaceImplParameterizedByRemovedClass: Interface<RemovedClass> {
    override val value: RemovedClass = TODO()
}

class Checker {
    fun useClassAsValueParameter(c: Class): String = "Checker.useClassAsValueParameter($c)"
    fun createAndPassClassAsValueParameter(): String = useClassAsValueParameter(Class())

    fun useRemovedClassAsValueParameter(@Suppress("UNUSED_PARAMETER") e: RemovedClass?): String = "FAIL: useRemovedClassAsValueParameter"
    fun createAndPassRemovedClassAsValueParameter(): String = useRemovedClassAsValueParameter(null)

    var removedClassProperty: RemovedClass? = null
        protected set(_) { /* Do nothing */ }

    fun writeToRemovedClassProperty(): String {
        removedClassProperty = null
        return "FAIL: writeToRemovedClassProperty"
    }

    fun createClass(): Class = Class()
    fun createClassAndCallFunction(): String = createClass().f()

    val getClass1: Class get() = Class()
    val getClassAndReadProperty1: String get() = getClass1.p

    val getClass2: Class = Class()
    val getClassAndReadProperty2: String = getClass2.p

    fun createRemovedClass(): RemovedClass = TODO()
    fun createRemovedClassAndCallFunction(): String = createRemovedClass().f()

    val getRemovedClass: RemovedClass get() = TODO()
    val getRemovedClassAndReadProperty: String get() = getRemovedClass.p

    class CrashesOnCreation {
        val getRemovedClass: RemovedClass = TODO()
        val getRemovedClassAndReadBar: String = getRemovedClass.p
    }

    fun createInterfaceImplParameterizedByClass(): Interface<Class> = InterfaceImplParameterizedByClass()
    fun createInterfaceImplParameterizedByClassAndCallFunction(): String = createInterfaceImplParameterizedByClass().value.f()

    fun createInstanceImplParameterizedByRemovedClass(): Interface<RemovedClass> = InterfaceImplParameterizedByRemovedClass()
    fun createInstanceImplParameterizedByRemovedClassAndCallFunction(): String = createInstanceImplParameterizedByRemovedClass().value.f()
}

fun readVariableInFunction() {
    var removed: RemovedClass? = null
    check(removed == null)
}

fun writeVariableInFunction() {
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "CanBeVal") var removed: RemovedClass?
    @Suppress("UNUSED_VALUE")
    removed = null
}

fun readVariableInLocalFunction() {
    fun local() {
        var removed: RemovedClass? = null
        check(removed == null)
    }
    local()
}

fun writeVariableInLocalFunction() {
    fun local() {
        @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "CanBeVal") var removed: RemovedClass?
        @Suppress("UNUSED_VALUE")
        removed = null
    }
    local()
}

fun readVariableInLocalClass() {
    class Local {
        fun foo() {
            var removed: RemovedClass? = null
            check(removed == null)
        }
    }
    Local().foo()
}

fun writeVariableInLocalClass() {
    class Local {
        fun foo() {
            @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "CanBeVal") var removed: RemovedClass?
            @Suppress("UNUSED_VALUE")
            removed = null
        }
    }
    Local().foo()
}

fun readVariableInAnonymousObject() {
    object {
        fun foo() {
            var removed: RemovedClass? = null
            check(removed == null)
        }
    }.foo()
}

fun writeVariableInAnonymousObject() {
    object {
        fun foo() {
            @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "CanBeVal") var removed: RemovedClass?
            @Suppress("UNUSED_VALUE")
            removed = null
        }
    }.foo()
}

fun readVariableInAnonymousObjectThroughLocalVar() {
    val obj = object {
        fun foo() {
            var removed: RemovedClass? = null
            check(removed == null)
        }
    }
    obj.foo()
}

fun writeVariableInAnonymousObjectThroughLocalVar() {
    val obj = object {
        fun foo() {
            @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "CanBeVal") var removed: RemovedClass?
            @Suppress("UNUSED_VALUE")
            removed = null
        }
    }
    obj.foo()
}

fun callLocalFunction() {
    fun local(): RemovedClass = TODO()
    local()
}

fun callLocalFunctionInLocalFunction() {
    fun local() {
        fun local(): RemovedClass = TODO()
        local()
    }
    local()
}

fun callLocalFunctionInFunctionOfLocalClass() {
    class Local {
        fun foo() {
            fun local(): RemovedClass = TODO()
            local()
        }
    }
    Local().foo()
}

fun callLocalFunctionInFunctionOfAnonymousObject() {
    object {
        fun foo() {
            fun local(): RemovedClass = TODO()
            local()
        }
    }.foo()
}

fun callLocalFunctionInFunctionOfAnonymousObjectThroughLocalVar() {
    val obj = object {
        fun foo() {
            fun local(): RemovedClass = TODO()
            local()
        }
    }
    obj.foo()
}

class TopLevelClassChildOfRemovedAbstractClass : RemovedAbstractClass()
object TopLevelObjectChildOfRemovedAbstractClass : RemovedAbstractClass()
interface TopLevelInterfaceChildOfRemovedInterface : RemovedInterface
class TopLevelClassChildOfRemovedInterface : RemovedInterface
object TopLevelObjectChildOfRemovedInterface : RemovedInterface
enum class TopLevelEnumClassChildOfRemovedInterface : RemovedInterface { ENTRY }

class TopLevel {
    class NestedClassChildOfRemovedAbstractClass : RemovedAbstractClass()
    object NestedObjectChildOfRemovedAbstractClass : RemovedAbstractClass()
    interface NestedInterfaceChildOfRemovedInterface : RemovedInterface
    class NestedClassChildOfRemovedInterface : RemovedInterface
    object NestedObjectChildOfRemovedInterface : RemovedInterface
    enum class NestedEnumClassChildOfRemovedInterface : RemovedInterface { ENTRY }

    inner class InnerClassChildOfRemovedAbstractClass : RemovedAbstractClass()
    inner class InnerClassChildOfRemovedInterface : RemovedInterface
}

class TopLevelWithCompanionChildOfRemovedAbstractClass {
    companion object : RemovedAbstractClass()
}

class TopLevelWithCompanionChildOfRemovedInterface {
    companion object : RemovedInterface
}

val anonymousObjectChildOfRemovedAbstractClass = object : RemovedAbstractClass() {}
val anonymousObjectChildOfRemovedInterface = object : RemovedInterface {}

fun topLevelFunctionWithLocalClassChildOfRemovedAbstractClass() {
    class LocalClass : RemovedAbstractClass()
    LocalClass().toString()
}

fun topLevelFunctionWithLocalClassChildOfRemovedInterface() {
    class LocalClass : RemovedInterface
    LocalClass().toString()
}

fun topLevelFunctionWithAnonymousObjectChildOfRemovedAbstractClass() {
    val anonymousObject = object : RemovedAbstractClass() {}
    anonymousObject.toString()
}

fun topLevelFunctionWithAnonymousObjectChildOfRemovedInterface() {
    val anonymousObject = object : RemovedInterface {}
    anonymousObject.toString()
}

open class OpenClassImpl : RemovedOpenClass()

inline fun inlinedFunctionWithRemovedOpenClassVariableType() {
    val foo: RemovedOpenClass? = null
    check(foo == null)
}

inline fun inlinedFunctionWithOpenClassImplVariableType() {
    val foo: OpenClassImpl? = null
    check(foo == null)
}

inline fun inlinedFunctionWithCreationOfRemovedOpenClass() {
    check(RemovedOpenClass().toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithCreationOfOpenClassImpl() {
    check(OpenClassImpl().toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithCreationOfRemovedOpenClassThroughReference() {
    check(run(::RemovedOpenClass).toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithCreationOfOpenClassImplThroughReference() {
    check(run(::OpenClassImpl).toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithRemovedOpenClassAnonymousObject() {
    val foo = object : RemovedOpenClass() {}
    check(foo.toString().isNotEmpty())
}

inline fun inlinedFunctionWithOpenClassImplAnonymousObject() {
    val foo = object : OpenClassImpl() {}
    check(foo.toString().isNotEmpty())
}
