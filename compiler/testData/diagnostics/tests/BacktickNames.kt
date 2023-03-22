// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
//  !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// TODO Uncomment all the examples when there will be no problems with light classes
//package `foo.bar`

// TODO: Uncomment after fixing KT-9416
//import kotlin.Deprecated as `deprecate\entity`

//@`deprecate\entity`("") data class Pair(val x: Int, val y: Int)

// Names should not contains characters: '.', ';', '[', ']', '/', '<', '>', ':', '\\'
//class `class.name`
class <!INVALID_CHARACTERS!>`class;name`<!>
class <!INVALID_CHARACTERS!>`class[name`<!>
class <!INVALID_CHARACTERS!>`class]name`<!>
//class `class/name`
class <!INVALID_CHARACTERS!>`class<name`<!>
class <!INVALID_CHARACTERS!>`class>name`<!>
class <!INVALID_CHARACTERS!>`class:name`<!>
class <!INVALID_CHARACTERS!>`class\name`<!>

class ` ` {}
class `  `

//val `val.X` = 10
val <!INVALID_CHARACTERS!>`val;X`<!> = 10
val <!INVALID_CHARACTERS!>`val[X`<!> = 10
val <!INVALID_CHARACTERS!>`val]X`<!> = 10
//val `val/X` = 10
val <!INVALID_CHARACTERS!>`val<X`<!> = 10
val <!INVALID_CHARACTERS!>`val>X`<!> = 10
val <!INVALID_CHARACTERS!>`val:X`<!> = 10
val <!INVALID_CHARACTERS!>`val\X`<!> = 10

val <!INVALID_CHARACTERS!>`;`<!> = 1
val <!INVALID_CHARACTERS!>`[`<!> = 2
val <!INVALID_CHARACTERS!>`]`<!> = 3
val <!INVALID_CHARACTERS!>`<`<!> = 4

val <!INVALID_CHARACTERS!>`>`<!> = 5
val <!INVALID_CHARACTERS!>`:`<!> = 6
val <!INVALID_CHARACTERS!>`\`<!> = 7
val <!INVALID_CHARACTERS!>`<>`<!> = 8

val <!INVALID_CHARACTERS!>`[]`<!> = 9
val <!INVALID_CHARACTERS!>`[;]`<!> = 10

// TODO Uncomment when there will be no problems with light classes (Error: Invalid formal type parameter (must be a valid Java identifier))
//class AWithTypeParameter<`T:K`> {}
//fun <`T/K`> genericFun(x: `T/K`) {}

class B(val <!INVALID_CHARACTERS!>`a:b`<!>: Int, val <!INVALID_CHARACTERS!>`c:d`<!>: Int)

val ff: (<!INVALID_CHARACTERS!>`x:X`<!>: Int) -> Unit = {}
val fg: ((<!INVALID_CHARACTERS!>`x:X`<!>: Int) -> Unit) -> Unit = {}
val fh: ((Int) -> ((<!INVALID_CHARACTERS!>`x:X`<!>: Int) -> Unit) -> Unit) = {{}}

fun f(x: Int, g: (Int) -> Unit) = g(x)

data class Data(val x: Int,  val y: Int)

class A() {
    init {
        val <!INVALID_CHARACTERS!>`a:b`<!> = 10
    }

    fun g(<!INVALID_CHARACTERS!>`x:y`<!>: Int) {
        val <!INVALID_CHARACTERS!>`s:`<!> = 30
    }
}

fun <!INVALID_CHARACTERS!>`foo:bar`<!>(<!INVALID_CHARACTERS!>`\arg`<!>: Int): Int {
    val (<!INVALID_CHARACTERS!>`a:b`<!>, c) = Data(10, 20)
    val <!INVALID_CHARACTERS!>`a\b`<!> = 10

    fun localFun() {}

    for (<!INVALID_CHARACTERS!>`x/y`<!> in 0..10) {
    }

    f(10) {
        <!INVALID_CHARACTERS!>`x:z`<!>: Int -> localFun()
    }

    f(20, fun(<!INVALID_CHARACTERS!>`x:z`<!>: Int): Unit {})

    try {
        val <!INVALID_CHARACTERS!>`a:`<!> = 10
    }
    catch (<!INVALID_CHARACTERS!>`e:a`<!>: Exception) {
        val <!INVALID_CHARACTERS!>`b:`<!> = 20
    }

    return `\arg`
}