// IGNORE_BACKEND_FIR: JVM_IR
interface  B<T> {
    val bar: T
}

fun String.foo() = object : B<String> {
    override val bar: String = length.toString()
}

class C {

    fun String.extension() = this.length

    fun String.fooInClass() = object : B<String> {
        override val bar: String = extension().toString()
    }

    fun String.fooInClassNoReceiver() = object : B<String> {
        override val bar: String = "123".extension().toString()
    }

    fun fooInClass(s: String) =  s.fooInClass().bar

    fun fooInClassNoReceiver(s: String) =  s.fooInClassNoReceiver().bar
}

fun box(): String {
    var result = "Hello, world!".foo().bar
    if (result != "13") return "fail 1: $result"

    result = C().fooInClass("Hello, world!")

    if (result != "13") return "fail 2: $result"

    result = C().fooInClassNoReceiver("Hello, world!")

    if (result != "3") return "fail 3: $result"

    return "OK"
}

