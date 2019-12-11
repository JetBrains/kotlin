//  !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// TODO Uncomment all the examples when there will be no problems with light classes
//package `foo.bar`

// TODO: Uncomment after fixing KT-9416
//import kotlin.Deprecated as `deprecate\entity`

//@`deprecate\entity`("") data class Pair(val x: Int, val y: Int)

// Names should not contains characters: '.', ';', '[', ']', '/', '<', '>', ':', '\\'
//class `class.name`
class `class;name`
class `class[name`
class `class]name`
//class `class/name`
class `class<name`
class `class>name`
class `class:name`
class `class\name`

class ` ` {}
class `  `

//val `val.X` = 10
val `val;X` = 10
val `val[X` = 10
val `val]X` = 10
//val `val/X` = 10
val `val<X` = 10
val `val>X` = 10
val `val:X` = 10
val `val\X` = 10

val `;` = 1
val `[` = 2
val `]` = 3
val `<` = 4

val `>` = 5
val `:` = 6
val `\` = 7
val `<>` = 8

val `[]` = 9
val `[;]` = 10

// TODO Uncomment when there will be no problems with light classes (Error: Invalid formal type parameter (must be a valid Java identifier))
//class AWithTypeParameter<`T:K`> {}
//fun <`T/K`> genericFun(x: `T/K`) {}

class B(val `a:b`: Int, val `c:d`: Int)

val ff: (`x:X`: Int) -> Unit = {}
val fg: ((`x:X`: Int) -> Unit) -> Unit = {}
val fh: ((Int) -> ((`x:X`: Int) -> Unit) -> Unit) = {{}}

fun f(x: Int, g: (Int) -> Unit) = g(x)

data class Data(val x: Int,  val y: Int)

class A() {
    init {
        val `a:b` = 10
    }

    fun g(`x:y`: Int) {
        val `s:` = 30
    }
}

fun `foo:bar`(`\arg`: Int): Int {
    val (`a:b`, c) = Data(10, 20)
    val `a\b` = 10

    fun localFun() {}

    for (`x/y` in 0..10) {
    }

    f(10) {
        `x:z`: Int -> localFun()
    }

    f(20, fun(`x:z`: Int): Unit {})

    try {
        val `a:` = 10
    }
    catch (`e:a`: Exception) {
        val `b:` = 20
    }

    return `\arg`
}