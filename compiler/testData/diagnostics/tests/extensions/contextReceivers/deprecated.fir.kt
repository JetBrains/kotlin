// LANGUAGE: +ContextReceivers

class A
class B

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
fun topLevelFun() {}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A, B)
fun topLevelFunTwoReceivers() {}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
var varProp: Int
    get() = 42
    set(newVal) {}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
val valProp: Int get() = 42

<!CONTEXT_RECEIVERS_DEPRECATED, CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
class Clazz {
    <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
    fun memberFun() {}

    <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
    fun B.memberExtFun() {}

    <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
    var varProp: Int
        get() = 42
        set(newVal) {}

    <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
    val valProp: Int get() = 42
}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
class Clazz2 {
    <!CONTEXT_RECEIVERS_DEPRECATED!>constructor()<!>
}

fun typeRef(body: <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A) () -> Unit): <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A) () -> Unit {
    val x: <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A) () -> Unit = body
    val y = body
    val z: suspend <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A) B.() -> Unit = {}
    val w: (<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(Int) () -> Unit, <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(Int) () -> Unit) -> (<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(Int) () -> Unit) = { a, b -> { } }
    return {}
}

typealias typealiasDecl = <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A) B.() -> Unit

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
fun Clazz.ext() {}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
val Clazz.extVal: Int get() = 904

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
var Clazz.extVar: Int
    get() = 904
    set(newVal) {}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
interface I {}

<!CONTEXT_RECEIVERS_DEPRECATED, CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A, B)
class ClazzTwoReceivers {}

<!CONTEXT_RECEIVERS_DEPRECATED, CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
enum class E

<!CONTEXT_RECEIVERS_DEPRECATED, CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
object O
