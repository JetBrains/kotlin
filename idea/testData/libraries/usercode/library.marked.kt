package testData.libraries

import java.util.*

trait SimpleTrait {
}

class SimpleClass {
}

class SimpleTraitImpl : SimpleTrait {
}

class <15>WithInnerAndObject {
    class object {
        fun <16>foo() {
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

enum class <4><5>Color(val <6>rgb : Int) {
    RED : Color(0xFF0000)
    GREEN : Color(0x00FF00)
    BLUE : Color(0x0000FF)
}

abstract class <1>ClassWithAbstractAndOpenMembers {
    abstract fun abstractFun()
    open fun openFun() {
    }

    abstract val abstractVal : String
    open val openVal : String = ""
    open val openValWithGetter : String
    get() {
        return "239"
    }

    public abstract var <2><3>abstractVar : String
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

public val <7>globalVal : #(Int, String) = #(239, "239")

val <8>globalValWithGetter : Long
get() {
    return System.currentTimeMillis()
}

val String.<9>exProp : String
get() {
    return this
}

val Int.exProp : Int
get() {
    return this
}

val <T> #(T, T).<10>exProp : String
get() {
    return "${this._1} : ${this._2}"
}

fun <11><12>func(a : Int, b : String = "55") {
}

fun <13>func(a : Int, b : Int) {
}

fun <14>func() {
}

inline fun <T> T.<17>filter(predicate: (T)-> Boolean) : T? = this
