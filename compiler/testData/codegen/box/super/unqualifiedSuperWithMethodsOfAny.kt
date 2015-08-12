interface ISomething

open class ClassWithToString {
    override fun toString(): String = "C"
}

interface IWithToString {
    override fun toString(): String
}

interface IWithDefaultToString {
    override fun toString(): String = "I"
}

class C1 : ClassWithToString(), ISomething {
    override fun toString(): String = super.toString()
}

class C2 : ClassWithToString(), IWithToString, ISomething  {
    override fun toString(): String = super.toString()
}

class C3 : IWithDefaultToString, ISomething  {
    override fun toString(): String = super.toString()
}

fun box(): String {
    return when {
        C1().toString() != "C" -> "Failed #1"
        C2().toString() != "C" -> "Failed #2"
        C3().toString() != "I" -> "Failed #3"
        else -> "OK"
    }
}