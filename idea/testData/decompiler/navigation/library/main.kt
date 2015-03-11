package testData.libraries

public trait SimpleTrait {
}

public class SimpleClass {
}

public class SimpleTraitImpl : SimpleTrait {
}

public class WithInnerAndObject {
    default object {
        fun foo() {
        }
    }

    class MyInner {
        trait MyInnerInner {
            fun innerInnerMethod()
        }
    }
}

public class WithTraitClassObject {
    default object : SimpleTrait
}

public abstract class AbstractClass {
}

public object NamedObject {
    public val objectMember: Int = 1
}

public enum class Color(val rgb : Int) {
    RED : Color(0xFF0000)
    GREEN : Color(0x00FF00)
    BLUE : Color(0x0000FF)
}

public abstract class ClassWithAbstractAndOpenMembers {
    public abstract fun abstractFun()
    public open fun openFun() {
    }

    public abstract val abstractVal : String
    public open val openVal : String = ""
    public open val openValWithGetter : String
    get() {
        return "239"
    }

    public abstract var abstractVar : String
    public open var openVar : String = ""
    public open var openVarWithGetter : String
    get() {
        return "239"
    }
    set(value) {
    }
}

public class ClassWithConstructor(val a: String, b: Any)

public fun main(args : Array<String>) {
}

public val globalVal : Pair<Int, String> = Pair(239, "239")

public val globalValWithGetter : Long
get() {
    return System.currentTimeMillis()
}

public val String.exProp : String
get() {
    return this
}

public val Int.exProp : Int
get() {
    return this
}

public class Pair<A, B>(val first: A, val second: B)

public val <T> Pair<T, T>.exProp : String
get() {
    return "${this.first} : ${this.second}"
}

public fun func(a : Int, b : String = "55") {
}

public fun func(a : Int, b : Int) {
}

public fun func() {
}

public fun func(cs : CharSequence) {
}

public fun <T> genericFunc() : T = throw Exception()

public inline fun <T> T.filter(predicate: (T)-> Boolean) : T? = this


public class Double

public fun processDouble(d: Double) {}

public fun processDouble(d: kotlin.Double) {}


public fun <T: CharSequence> funWithTypeParam(t: T) {
}

public fun <T: Number> funWithTypeParam(t: T) {
}
