// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

class A {
    companion object {
        fun foo(): Int = 43
        val companionProp: Int = 44
    }

    fun baz(): Int = 1
    val memberProp: Int = 2
}

object Obj {
    fun foo(): Int = 43
    val objProp: Int = 44
}

fun main() {
    <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>A::foo<!>.invoke(A())
    <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>A::foo<!>.invoke<!NO_VALUE_FOR_PARAMETER!>()<!>
    val x = <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>A::foo<!>
    x.invoke(A())
    x.invoke<!NO_VALUE_FOR_PARAMETER!>()<!>

    A.Companion::foo.invoke()
    val x0 = A.Companion::foo
    x0.invoke()

    bar(A::foo)

    val y = id(A::foo)
    y.invoke()

    A::baz.invoke(A())

    val z = A::baz
    z.invoke(A())
    bam(A::baz)

    Obj::foo.invoke()

    val zObj = Obj::foo
    zObj.invoke()
    bar(Obj::foo)
}

fun mainProp() {
    <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>A::companionProp<!>.invoke(A())
    <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>A::companionProp<!>.invoke<!NO_VALUE_FOR_PARAMETER!>()<!>
    val x = <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>A::companionProp<!>
    x.invoke(A())
    x.invoke<!NO_VALUE_FOR_PARAMETER!>()<!>

    A.Companion::companionProp.invoke()
    val x0 = A.Companion::companionProp
    x0.invoke()

    bar(A::companionProp)

    val y = id(A::companionProp)
    y.invoke()

    A::memberProp.invoke(A())

    val z = A::memberProp
    z.invoke(A())
    bam(A::memberProp)

    Obj::objProp.invoke()

    val zObj = Obj::objProp
    zObj.invoke()
    bar(Obj::objProp)
}

fun <E> id(e: E): E = e

fun bar(x: () -> Int) {}
fun bam(x: A.() -> Int) {}
