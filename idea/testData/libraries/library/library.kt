package testData.libraries

import java.util.*

trait SimpleTrait {
}

class SimpleClass {
}

class SimpleTraitImpl : SimpleTrait {
}

class WithInnerAndObject {
    class object {
        fun foo() {
        }
    }

    class MyInner {
        trait MyInnerInner {
            fun innerInnerMethod()
        }
    }
}

class WithTraitClassObject {
    class object : SimpleTrait
}

abstract class AbstractClass {
}

enum class Color(val rgb : Int) {
    RED : Color(0xFF0000)
    GREEN : Color(0x00FF00)
    BLUE : Color(0x0000FF)
}

abstract class ClassWithAbstractAndOpenMembers {
    abstract fun abstractFun()
    open fun openFun() {
    }

    abstract val abstractVal : String
    open val openVal : String = ""
    open val openValWithGetter : String
    get() {
        return "239"
    }

    public abstract var abstractVar : String
    open var openVar : String = ""
    open var openVarWithGetter : String
    get() {
        return "239"
    }
    set(value) {
    }
}

fun main(args : Array<String>) {
}

public val globalVal : #(Int, String) = #(239, "239")

val globalValWithGetter : Long
get() {
    return System.currentTimeMillis()
}

val String.exProp : String
get() {
    return this
}

val Int.exProp : Int
get() {
    return this
}

val <T> #(T, T).exProp : String
get() {
    return "${this._1} : ${this._2}"
}

fun func(a : Int, b : String = "55") {
}

fun func(a : Int, b : Int) {
}

fun func() {
}

inline fun <T> T.filter(predicate: (T)-> Boolean) : T? = this
