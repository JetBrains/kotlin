// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:JvmExposeBoxed<!>

@JvmExposeBoxed("foo")
class Foo
@JvmExposeBoxed("foo") constructor()
{
    @JvmExposeBoxed("foo")
    companion object {

    }
}

@JvmExposeBoxed("foo")
class Obj

@JvmExposeBoxed
@JvmInline
value class IC(val s: String)

<!WRONG_ANNOTATION_TARGET!>@JvmExposeBoxed<!>
val ic: IC = TODO()

@get:JvmExposeBoxed
val icgetter: IC = TODO()

val icgetter2: IC
    @JvmExposeBoxed get() = TODO()

@set:JvmExposeBoxed
var icsetter: IC = TODO()

var icsetter2: IC
    get() = TODO()
    @JvmExposeBoxed set(value) {}

@get:JvmExposeBoxed("foo")
val icic: IC = TODO()

@JvmExposeBoxed
fun foo(): IC = TODO()

@JvmExposeBoxed
fun bar(ic: IC) {}

@JvmExposeBoxed
fun foo(result: Result<Any>) {}

class C {
    @JvmExposeBoxed
    fun foo(): IC = TODO()

    @JvmExposeBoxed
    fun foo(result: Result<Any>) {}

    <!WRONG_ANNOTATION_TARGET!>@JvmExposeBoxed<!>
    val ic: IC = TODO()

    @get:JvmExposeBoxed
    val icgetter: IC = TODO()

    val icgetter2: IC
        @JvmExposeBoxed get() = TODO()

    @set:JvmExposeBoxed
    var icsetter: IC = TODO()

    var icsetter2: IC
        get() = TODO()
        @JvmExposeBoxed set(value) {}
}

@JvmExposeBoxed
fun Int.foo(i: Int) {}

@JvmExposeBoxed("...")
fun todo(ic: IC) {}

@JvmExposeBoxed
inline fun <reified T> inlineMe(ic: IC) {}

@JvmExposeBoxed("same")
fun same(): IC = TODO()

@JvmExposeBoxed
suspend fun suspendMe(ic: IC) {}

@JvmExposeBoxed("foo" + "bar")
fun foobar(): IC = TODO()

@JvmExposeBoxed
class WithIC {
    fun acceptsIC(ic: IC) {}

    fun returnsIC(): IC = TODO()

    val ic: IC = TODO()

    val icgetter: IC = TODO()

    val icgetter2: IC
        get() = TODO()

    var icsetter: IC = TODO()

    var icsetter2: IC
        get() = TODO()
        set(value) {}
}
