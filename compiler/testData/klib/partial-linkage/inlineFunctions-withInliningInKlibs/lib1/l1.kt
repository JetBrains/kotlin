@file:Suppress("NOTHING_TO_INLINE")

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**************************************************/
/***** Extracted from 'classTransformations': *****/
/**************************************************/

class Class {
    fun f() = "FAIL: Class.f"
    val p get() = "FAIL: Class.p"
    override fun toString() = "FAIL: Class.toString"
}

class ClassToEnum {
    class Foo
    object Bar
    inner class Baz
}

object ObjectToEnum {
    class Foo
    object Bar
}

enum class EnumToClass {
    Foo,
    Bar,
    Baz
}

enum class EnumToObject {
    Foo,
    Bar
}

class ClassToObject
object ObjectToClass

class ClassToInterface

class NestedObjectToCompanion1 {
    object Companion {
        fun name() = "NestedObjectToCompanion1.Companion"
        override fun toString() = name()
    }
}

class NestedObjectToCompanion2 {
    object Foo {
        fun name() = "NestedObjectToCompanion2.Foo"
        override fun toString() = name()
    }
}

class CompanionToNestedObject1 {
    companion object {
        fun name() = "CompanionToNestedObject1.Companion"
        override fun toString() = name()
    }
}

class CompanionToNestedObject2 {
    companion object Foo {
        fun name() = "CompanionToNestedObject2.Foo"
        override fun toString() = name()
    }
}

class CompanionAndNestedObjectsSwap {
    companion object Foo {
        fun name() = "Foo"
    }

    object Bar {
        fun name() = "Bar"
    }
}

class NestedClassContainer {
    fun name() = "NestedClassContainer"

    class NestedToInner {
        fun name() = "NestedClassContainer.NestedToInner"
        override fun toString() = name()

        object Object {
            fun name() = "NestedClassContainer.NestedToInner.Object"
            override fun toString() = name()
        }

        companion object Companion {
            fun name() = "NestedClassContainer.NestedToInner.Companion"
            override fun toString() = name()
        }

        class Nested {
            fun name() = "NestedClassContainer.NestedToInner.Nested"
            override fun toString() = name()
        }

        inner class Inner {
            fun name() = this@NestedToInner.name() + ".Inner"
            override fun toString() = name()
        }
    }
}

class InnerClassContainer {
    fun name() = "InnerClassContainer"

    inner class InnerToNested {
        fun name() = this@InnerClassContainer.name() + ".InnerToNested"
        override fun toString() = name()

        inner class /*object*/ Object {
            fun name() = this@InnerToNested.name() + ".Object"
            override fun toString() = name()
        }

        inner class /*companion object*/ Companion {
            fun name() = this@InnerToNested.name() + ".Companion"
            override fun toString() = name()
        }

        inner class /*class*/ Nested {
            fun name() = this@InnerToNested.name() + ".Nested"
            override fun toString() = name()
        }

        inner class Inner {
            fun name() = this@InnerToNested.name() + ".Inner"
            override fun toString() = name()
        }
    }
}

annotation class AnnotationClassWithChangedParameterType(val x: Int)
annotation class AnnotationClassThatBecomesRegularClass(val x: Int)
annotation class AnnotationClassThatDisappears(val x: Int)
annotation class AnnotationClassWithRenamedParameters(val i: Int, val s: String)
annotation class AnnotationClassWithReorderedParameters(val i: Int, val s: String)
annotation class AnnotationClassWithNewParameter(val i: Int)

value class ValueToClass(val x: Int)
class ClassToValue(val x: Int)

data class DataToClass(val x: Int, val y: Int)

class ClassToAbstractClass {
    var name: String = "Alice"
    fun getGreeting() = "Hello, $name!"
}

class RemovedClass {
    fun f() = "FAIL: RemovedClass.f"
    val p get() = "FAIL: RemovedClass.p"
}
enum class EnumClassWithDisappearingEntry { UNCHANGED, REMOVED }

object PublicTopLevelLib1 {
    annotation class AnnotationClassThatBecomesPrivate
    class ClassThatBecomesPrivate
    enum class EnumClassThatBecomesPrivate { ENTRY }
}

interface XAnswer { fun answer(): Int }
interface XAnswerDefault { fun answer(): Int /*= 42*/ }
interface XFunction1 { /*fun function1(): Int*/ }
interface XFunction1Default { /*fun function1(): Int = 42*/ }
interface XFunction2 { /*fun function2(): Int*/ }
interface XFunction2Default { /*fun function2(): Int = -42*/ }
interface XProperty1 { /*val property1: Int*/ }
interface XProperty1Default { /*val property1: Int get() = 42*/ }
interface XProperty2 { /*val property2: Int*/ }
interface XProperty2Default { /*val property2: Int get() = 42*/ }

fun interface FunctionalInterfaceToInterface : XAnswer

/*****************************************************/
/***** Extracted from 'functionTransformations': *****/
/*****************************************************/

object Functions {
    @Suppress("RedundantSuspendModifier") suspend fun <R> wrapCoroutine(coroutine: suspend () -> R): R = coroutine.invoke()
    suspend fun suspendToNonSuspendFunction(x: Int): Int = wrapCoroutine { -x }
    fun nonSuspendToSuspendFunction(x: Int): Int = -x

    inline fun inlineLambdaToNoinlineLambda(x: Int, lambda: (Int) -> String): String = "Functions.inlineLambdaToNoinlineLambda($x) { ${lambda(x * 2)} }"
    inline fun inlineLambdaToCrossinlineLambda(x: Int, lambda: (Int) -> String): String = "Functions.inlineLambdaToCrossinlineLambda($x) { ${lambda(x * 2)} }"
}

open class OpenClass {
    open fun openNonInlineToInlineFunction(x: Int): String = "OpenClass.openNonInlineToInlineFunction($x)"
    open fun openNonInlineToInlineFunctionWithDelegation(x: Int): String = "OpenClass.openNonInlineToInlineFunctionWithDelegation($x)"
    //inline fun newInlineFunction1(x: Int): String = "OpenClass.newInlineFunction1($x)"
    //inline fun newInlineFunction2(x: Int): String = "OpenClass.newInlineFunction2($x)"
    //fun newNonInlineFunction(x: Int): String = "OpenClass.newNonInlineFunction($x)"

    fun newInlineFunction1Caller(x: Int): String = TODO("Not implemented: OpenClass.newInlineFunction1Caller($x)")
    fun newInlineFunction2Caller(x: Int): String = TODO("Not implemented: OpenClass.newInlineFunction2Caller($x)")
    fun newNonInlineFunctionCaller(x: Int): String = TODO("Not implemented: OpenClass.newNonInlineFunctionCaller($x)")
}

/********************************************/
/***** Extracted from 'removeCallable': *****/
/********************************************/

fun removedFunction(): String = "FAIL: removedFunction"
val removedProperty: String get() = "FAIL: removedProperty"

/*****************************************/
/***** Extracted from 'removeClass': *****/
/*****************************************/

abstract class RemovedAbstractClass {
    abstract fun abstractFun(): String
    open fun openFun(): String = "RemovedAbstractClass.openFun"
    fun finalFun(): String = "RemovedAbstractClass.finalFun"
    abstract val abstractVal: String
    open val openVal: String get() = "RemovedAbstractClass.openVal"
    val finalVal: String get() = "RemovedAbstractClass.finalVal"
}

interface RemovedInterface {
    fun abstractFun(): String
    fun abstractFunWithDefaultImpl(): String = "RemovedInterface.abstractFunWithDefaultImpl"
    val abstractVal: String
    val abstractValWithDefaultImpl: String get() = "RemovedInterface.abstractValWithDefaultImpl"
}

open class RemovedOpenClass {
    open fun openFun(): String = "RemovedOpenClass.openFun"
    fun finalFun(): String = "RemovedOpenClass.finalFun"
    open val openVal: String get() = "RemovedOpenClass.openVal"
    val finalVal: String get() = "RemovedOpenClass.finalVal"
}

/***********************************************/
/***** Extracted from 'inheritanceIssues': *****/
/***********************************************/

interface InterfaceToAbstractClass
interface InterfaceToOpenClass
interface InterfaceToFinalClass
interface InterfaceToAnnotationClass
interface InterfaceToObject
interface InterfaceToEnumClass
interface InterfaceToValueClass
interface InterfaceToDataClass

open class OpenClassToFinalClass(val x: Int)
open class OpenClassToAnnotationClass(val x: Int)
open class OpenClassToObject(val x: Int)
open class OpenClassToEnumClass(val x: Int)
open class OpenClassToValueClass(val x: Int)
open class OpenClassToDataClass(val x: Int)
open class OpenClassToInterface(val x: Int)

interface InterfaceToAbstractClass1
interface InterfaceToAbstractClass2
abstract class AbstractClass

abstract class AbstractClassWithChangedConstructorSignature(name: String) {
    val greeting = "Hello, $name!"
}

open class OpenClassWithChangedConstructorSignature(name: String) {
    val greeting = "Hello, $name!"
}

open class SuperSuperClass {
    open fun inheritsFrom() = "SuperSuperClass -> Any"
}
open class SuperClass : SuperSuperClass() {
    override fun inheritsFrom() = "SuperClass -> " + super.inheritsFrom()
}
class SuperSuperClassReplacedBySuperClass : SuperSuperClass() {
    override fun inheritsFrom() = "SuperSuperClassReplacedBySuperClass -> " + super.inheritsFrom()
}
class SuperClassReplacedBySuperSuperClass : SuperClass() {
    override fun inheritsFrom() = "SuperClassReplacedBySuperSuperClass -> " + super.inheritsFrom()
}

/*************************************/
/***** Extracted from 'kt73511': *****/
/*************************************/

@Target(CLASS)
@Retention(BINARY)
public annotation class MyAnnotationMarker(
    val markerClass: KClass<out Annotation>
)

/******************************************************/
/***** Extracted from 'richReferencesOperations': *****/
/******************************************************/

inline fun removedInlineFun(x: Int): Int = x

inline val removedInlineVal: Int
    get() = 321

inline var removedInlineVar: Int
    get() = 231
    set(_) = Unit
