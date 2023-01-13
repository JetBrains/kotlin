package two

import java.lang.Runnable

interface BaseInterface
interface NonBaseInterface : BaseInterface
interface AnotherInterface

abstract class BaseClass
abstract class NonBaseClass : BaseClass()

object Object

object ObjectWithInterface : BaseInterface

object ObjectWithNonBaseInterface : NonBaseInterface

object ObjectWithClass : BaseClass()

object ObjectWithClassAndInterface : NonBaseClass(), NonBaseInterface
object ObjectWithClassAndJavaInterface : NonBaseClass(), Runnable {
    override fun run() {}
}

val a = object : BaseClass() {}
val b = object : NonBaseClass() {}
val c = object : BaseInterface {}
val d = object : NonBaseInterface {}
val e: NonBaseInterface = object : BaseClass(), NonBaseInterface, AnotherInterface {}
val f: AnotherInterface = object : BaseInterface, AnotherInterface {}