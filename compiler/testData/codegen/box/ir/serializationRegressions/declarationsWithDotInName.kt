// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt
package lib

// Top-level declarations with characters valid in JS identifiers, other than a dot
fun ` public `(): String = "public"

fun `dash-function`(): String = "dash"

fun `plus+function`(): String = "plus"

val `top-level property`: String = "top"

var `top-level var`: String = ""

fun String.`extension-function`(): String = this + "!"

val String.`extension property`: String
    get() = this + "?"

class `Space Class`(val value: String)

object `Dash-Object` {
    val value: String = "object"
}

interface `Interface-Name` {
    fun get(): String
}

class `Impl-Class` : `Interface-Name` {
    override fun get(): String = "impl"
}

enum class `Enum-Class` {
    `entry one`,
    `entry.two`,
    ` .entry three. `,
}

typealias ` .Alias. ` = `Space Class`

// Members with dots in various positions
class Shapes {
    fun ` .leading`(): String = "leading"
    fun `trailing.`(): String = "trailing"
    fun `.`(): String = "dot"
    fun `a.b.c`(): String = "abc"
    fun ` mixed .-+ name `(): String = "mixed"

    class ` .Nested. ` {
        val value: String = "nested"
    }
}

// Members
class Outer {
    fun `member.function`(): String = "function"

    class `Nested.class` {
        val value: String = "nested"
    }

    inner class `Inner.class` {
        val value: String = "inner"
    }

    companion object `Companion.object` {
        val value: String = "companion"
    }
}

// Enum entries
enum class Letter {
    `A.a`,
    `B.b`,
}

// Type aliases
class Real(val v: String)

typealias `Real.alias` = Real

typealias `List.alias`<T> = List<T>

fun makeAlias(v: String): `Real.alias` = Real(v)

fun aliasValue(x: `Real.alias`): String = x.v

fun makeListAlias(): `List.alias`<String> = listOf("a", "b")

// Type parameters
fun <`T.p`> identity(x: `T.p`): `T.p` = x

class Container<`T.p`>(val value: `T.p`) {
    fun <`R.r`> map(f: (`T.p`) -> `R.r`): Container<`R.r`> = Container(f(value))
}

inline fun <reified `T.reified`> isInstance(x: Any): Boolean = x is `T.reified`

// Value parameters
fun concat(`first.param`: String, `second.param`: String = "default"): String = `first.param` + `second.param`

class WithParams(`first.ctor.param`: String, `second.ctor.param`: String) {
    val combined: String = `first.ctor.param` + `second.ctor.param`
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    // Members
    val outer = Outer()
    if (outer.`member.function`() != "function") return "Fail member function: ${outer.`member.function`()}"
    if (Outer.`Nested.class`().value != "nested") return "Fail nested class"
    if (outer.`Inner.class`().value != "inner") return "Fail inner class: ${outer.`Inner.class`().value}"
    if (Outer.`Companion.object`.value != "companion") return "Fail companion object: ${Outer.`Companion.object`.value}"

    // Top-level declarations with characters invalid in JS identifiers, other than a dot
    if (` public `() != "public") return "Fail function with spaces"
    if (`dash-function`() != "dash") return "Fail function with dash"
    if (`plus+function`() != "plus") return "Fail function with plus"
    if (`top-level property` != "top") return "Fail top-level property: ${`top-level property`}"
    `top-level var` = "set"
    if (`top-level var` != "set") return "Fail top-level var: ${`top-level var`}"
    if ("a".`extension-function`() != "a!") return "Fail extension function"
    if ("a".`extension property` != "a?") return "Fail extension property"
    if (`Space Class`("space").value != "space") return "Fail class with space"
    if (`Dash-Object`.value != "object") return "Fail object with dash"
    val impl: `Interface-Name` = `Impl-Class`()
    if (impl.get() != "impl") return "Fail interface implementation"
    if (`Enum-Class`.entries.map { it.name } != listOf("entry one", "entry.two", " .entry three. ")) return "Fail enum: ${`Enum-Class`.entries}"
    if (`Enum-Class`.valueOf(" .entry three. ") != `Enum-Class`.` .entry three. `) return "Fail enum valueOf"
    val aliased: ` .Alias. ` = ` .Alias. `("alias")
    if (aliased.value != "alias") return "Fail type alias with dots"

    // Members with dots in various positions
    val shapes = Shapes()
    if (shapes.` .leading`() != "leading") return "Fail leading dot"
    if (shapes.`trailing.`() != "trailing") return "Fail trailing dot"
    if (shapes.`.`() != "dot") return "Fail single dot"
    if (shapes.`a.b.c`() != "abc") return "Fail several dots"
    if (shapes.` mixed .-+ name `() != "mixed") return "Fail mixed characters"
    if (Shapes.` .Nested. `().value != "nested") return "Fail nested class with dots"

    // Enum entries
    if (Letter.`A.a`.name != "A.a") return "Fail enum entry name: ${Letter.`A.a`.name}"
    if (Letter.valueOf("B.b") != Letter.`B.b`) return "Fail enum valueOf"
    if (Letter.entries.map { it.name } != listOf("A.a", "B.b")) return "Fail enum entries: ${Letter.entries}"
    val letter: Letter = Letter.`B.b`
    val whenResult = when (letter) {
        Letter.`A.a` -> "a"
        Letter.`B.b` -> "b"
    }
    if (whenResult != "b") return "Fail enum when: $whenResult"

    // Type aliases
    val alias: `Real.alias` = `Real.alias`("alias")
    if (alias.v != "alias") return "Fail type alias: ${alias.v}"
    val list: `List.alias`<Int> = listOf(1, 2)
    if (list.size != 2) return "Fail generic type alias: $list"
    if (aliasValue(makeAlias("sig")) != "sig") return "Fail type alias in signatures"
    if (makeListAlias() != listOf("a", "b")) return "Fail generic type alias in signatures: ${makeListAlias()}"

    // Type parameters
    if (identity("id") != "id") return "Fail generic function"
    if (Container(21).map { it * 2 }.value != 42) return "Fail generic class"
    if (!isInstance<String>("s") || isInstance<Int>("s")) return "Fail reified type parameter"

    // Value parameters
    if (concat(`second.param` = "B", `first.param` = "A") != "AB") return "Fail named value parameters"
    if (concat("A") != "Adefault") return "Fail default value parameter"
    val withParams = WithParams(`second.ctor.param` = "2", `first.ctor.param` = "1")
    if (withParams.combined != "12") return "Fail constructor parameters: ${withParams.combined}"

    // Local declarations
    fun `local.function`(): String = "local"
    class `Local.class`(val value: String)
    if (`Local.class`(`local.function`()).value != "local") return "Fail local class and function"
    val (`destructured.first`, `destructured.second`) = Pair(1, 2)
    if (`destructured.first` + `destructured.second` != 3) return "Fail destructuring"
    val `local.val` = "O"
    val lambda = { `lambda.param`: String -> `lambda.param` + "K" }
    return lambda(`local.val`)
}
