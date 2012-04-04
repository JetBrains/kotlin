package testData.libraries

import java.util.*

public trait SimpleTrait {
}

public class SimpleClass {
}

public class SimpleTraitImpl : SimpleTrait {
}

public class <15>WithInnerAndObject {
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

public class WithTraitClassObject {
    class object : SimpleTrait
}

public abstract class AbstractClass {
}

public enum class <4><5>Color(val <6>rgb : Int) {
    RED : Color(0xFF0000)
    GREEN : Color(0x00FF00)
    BLUE : Color(0x0000FF)
}

public abstract class <1>ClassWithAbstractAndOpenMembers {
    public abstract fun abstractFun()
    public open fun openFun() {
    }

    public abstract val abstractVal : String
    public open val openVal : String = ""
    public open val openValWithGetter : String
    get() {
        return "239"
    }

    public abstract var <2><3>abstractVar : String
    public open var openVar : String = ""
    public open var openVarWithGetter : String
    get() {
        return "239"
    }
    set(value) {
    }
}

public fun main(args : Array<String>) {
}

public val <7>globalVal : #(Int, String) = #(239, "239")

public val <8>globalValWithGetter : Long
get() {
    return System.currentTimeMillis()
}

public val String.<9>exProp : String
get() {
    return this
}

public val Int.exProp : Int
get() {
    return this
}

public val <T> #(T, T).<10>exProp : String
get() {
    return "${this._1} : ${this._2}"
}

public fun <11><12>func(a : Int, b : String = "55") {
}

public fun <13>func(a : Int, b : Int) {
}

public fun <14>func() {
}

public inline fun <T> T.<17>filter(predicate: (T)-> Boolean) : T? = this
