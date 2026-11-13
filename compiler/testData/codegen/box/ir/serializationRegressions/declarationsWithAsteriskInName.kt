// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// WITH_STDLIB
// Unlike a dot, an asterisk doesn't break klib linkage in any declaration name:
// top-level classes, functions and properties, members, local declarations, enum entries, type aliases,
// type and value parameters may all have asterisks in their names.
// JsAllowInvalidCharsIdentifiersEscaping lets top-level declarations use characters invalid in JS identifiers.

// MODULE: lib
// FILE: lib.kt
package lib

// Top-level declarations with characters invalid in JS identifiers, other than an asterisk
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
    `entry*two`,
    ` *entry three* `,
}

typealias ` *Alias* ` = `Space Class`

// Top-level declarations with asterisks, which are forbidden to have dots in their names
fun `top*function`(): String = "function"

fun ` *leading top function`(): String = "leading"

fun `trailing top function*`(): String = "trailing"

fun `*`(): String = "single"

fun String.`extension*function`(): String = this + "#"

val `top*property`: String = "property"

var `top*var`: String = ""

val String.`extension*property`: String
    get() = this + "&"

const val `top*const`: String = "const"

class `Top*Class`(val value: String)

interface `Top*Interface` {
    fun get(): String
}

class `Top*Impl` : `Top*Interface` {
    override fun get(): String = "impl"
}

object `Top*Object` {
    val value: String = "object"
}

annotation class `Top*Annotation`

@`Top*Annotation`
fun annotatedTop(): String = "annotated"

enum class `Top*Enum` { A, B }

data class `Top*Data`(val value: String)

// Members with asterisks in various positions
class Shapes {
    fun ` *leading`(): String = "leading"
    fun `trailing*`(): String = "trailing"
    fun `*`(): String = "asterisk"
    fun `a*b*c`(): String = "abc"
    fun ` mixed *-+ name `(): String = "mixed"
    val ` *leading property`: String = "leading property"
    val `trailing property*`: String = "trailing property"

    class ` *Nested* ` {
        val value: String = "nested"
    }
}

// Members
class Outer {
    val `member*property` = 1
    var `member*var`: String = ""
    fun `member*function`(): String = "function"

    class `Nested*class` {
        val value: String = "nested"
    }

    inner class `Inner*class` {
        val value: String = "inner" + `member*property`
    }

    companion object `Companion*object` {
        val `companion*property`: String = "companion"
    }
}

object Holder {
    val `object*property`: String = "object"
    const val `const*property`: String = "const"
}

// Enum entries
enum class Letter {
    `A*a`,
    `B*b`,
}

// Type aliases
class Real(val v: String)

typealias `Real*alias` = Real

typealias `List*alias`<T> = List<T>

fun makeAlias(v: String): `Real*alias` = Real(v)

fun aliasValue(x: `Real*alias`): String = x.v

fun makeListAlias(): `List*alias`<String> = listOf("a", "b")

// Type parameters
fun <`T*p`> identity(x: `T*p`): `T*p` = x

class Container<`T*p`>(val value: `T*p`) {
    fun <`R*r`> map(f: (`T*p`) -> `R*r`): Container<`R*r`> = Container(f(value))
}

inline fun <reified `T*reified`> isInstance(x: Any): Boolean = x is `T*reified`

// Value parameters
fun concat(`first*param`: String, `second*param`: String = "default"): String = `first*param` + `second*param`

class WithParams(`first*ctor*param`: String, `second*ctor*param`: String) {
    val combined: String = `first*ctor*param` + `second*ctor*param`
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    // Members
    val outer = Outer()
    if (outer.`member*property` != 1) return "Fail member property: ${outer.`member*property`}"
    outer.`member*var` = "set"
    if (outer.`member*var` != "set") return "Fail member var: ${outer.`member*var`}"
    if (outer.`member*function`() != "function") return "Fail member function: ${outer.`member*function`()}"
    if (Outer.`Nested*class`().value != "nested") return "Fail nested class"
    if (outer.`Inner*class`().value != "inner1") return "Fail inner class: ${outer.`Inner*class`().value}"
    if (Outer.`companion*property` != "companion") return "Fail companion property: ${Outer.`companion*property`}"
    if (Holder.`object*property` != "object") return "Fail object property: ${Holder.`object*property`}"
    if (Holder.`const*property` != "const") return "Fail const property: ${Holder.`const*property`}"

    // Top-level declarations with characters invalid in JS identifiers, other than an asterisk
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
    if (`Enum-Class`.entries.map { it.name } != listOf("entry one", "entry*two", " *entry three* ")) return "Fail enum: ${`Enum-Class`.entries}"
    if (`Enum-Class`.valueOf(" *entry three* ") != `Enum-Class`.` *entry three* `) return "Fail enum valueOf"
    val aliased: ` *Alias* ` = ` *Alias* `("alias")
    if (aliased.value != "alias") return "Fail type alias with asterisks"

    // Top-level declarations with asterisks, which are forbidden to have dots in their names
    if (`top*function`() != "function") return "Fail top-level function with an asterisk"
    if (` *leading top function`() != "leading") return "Fail top-level function with a leading asterisk"
    if (`trailing top function*`() != "trailing") return "Fail top-level function with a trailing asterisk"
    if (`*`() != "single") return "Fail top-level function named an asterisk"
    if ("a".`extension*function`() != "a#") return "Fail top-level extension function with an asterisk"
    if (`top*property` != "property") return "Fail top-level property with an asterisk"
    `top*var` = "set"
    if (`top*var` != "set") return "Fail top-level var with an asterisk"
    if ("a".`extension*property` != "a&") return "Fail top-level extension property with an asterisk"
    if (`top*const` != "const") return "Fail top-level const with an asterisk"
    if (`Top*Class`("class").value != "class") return "Fail top-level class with an asterisk"
    val topImpl: `Top*Interface` = `Top*Impl`()
    if (topImpl.get() != "impl") return "Fail top-level interface with an asterisk"
    if (`Top*Object`.value != "object") return "Fail top-level object with an asterisk"
    if (annotatedTop() != "annotated") return "Fail top-level annotation class with an asterisk"
    if (`Top*Enum`.entries.map { it.name } != listOf("A", "B")) return "Fail top-level enum class with an asterisk"
    val topData = `Top*Data`("data")
    if (topData != `Top*Data`("data")) return "Fail top-level data class with an asterisk"
    if (topData.toString() != "Top*Data(value=data)") return "Fail top-level data class toString: $topData"

    // Members with asterisks in various positions
    val shapes = Shapes()
    if (shapes.` *leading`() != "leading") return "Fail leading asterisk"
    if (shapes.`trailing*`() != "trailing") return "Fail trailing asterisk"
    if (shapes.`*`() != "asterisk") return "Fail single asterisk"
    if (shapes.`a*b*c`() != "abc") return "Fail several asterisks"
    if (shapes.` mixed *-+ name `() != "mixed") return "Fail mixed characters"
    if (shapes.` *leading property` != "leading property") return "Fail leading asterisk property"
    if (shapes.`trailing property*` != "trailing property") return "Fail trailing asterisk property"
    if (Shapes.` *Nested* `().value != "nested") return "Fail nested class with asterisks"

    // Enum entries
    if (Letter.`A*a`.name != "A*a") return "Fail enum entry name: ${Letter.`A*a`.name}"
    if (Letter.valueOf("B*b") != Letter.`B*b`) return "Fail enum valueOf"
    if (Letter.entries.map { it.name } != listOf("A*a", "B*b")) return "Fail enum entries: ${Letter.entries}"
    val letter: Letter = Letter.`B*b`
    val whenResult = when (letter) {
        Letter.`A*a` -> "a"
        Letter.`B*b` -> "b"
    }
    if (whenResult != "b") return "Fail enum when: $whenResult"

    // Type aliases
    val alias: `Real*alias` = `Real*alias`("alias")
    if (alias.v != "alias") return "Fail type alias: ${alias.v}"
    val list: `List*alias`<Int> = listOf(1, 2)
    if (list.size != 2) return "Fail generic type alias: $list"
    if (aliasValue(makeAlias("sig")) != "sig") return "Fail type alias in signatures"
    if (makeListAlias() != listOf("a", "b")) return "Fail generic type alias in signatures: ${makeListAlias()}"

    // Type parameters
    if (identity("id") != "id") return "Fail generic function"
    if (Container(21).map { it * 2 }.value != 42) return "Fail generic class"
    if (!isInstance<String>("s") || isInstance<Int>("s")) return "Fail reified type parameter"

    // Value parameters
    if (concat(`second*param` = "B", `first*param` = "A") != "AB") return "Fail named value parameters"
    if (concat("A") != "Adefault") return "Fail default value parameter"
    val withParams = WithParams(`second*ctor*param` = "2", `first*ctor*param` = "1")
    if (withParams.combined != "12") return "Fail constructor parameters: ${withParams.combined}"

    // Local declarations
    fun `local*function`(): String = "local"
    class `Local*class`(val value: String)
    if (`Local*class`(`local*function`()).value != "local") return "Fail local class and function"
    val (`destructured*first`, `destructured*second`) = Pair(1, 2)
    if (`destructured*first` + `destructured*second` != 3) return "Fail destructuring"
    val `local*val` = "O"
    val lambda = { `lambda*param`: String -> `lambda*param` + "K" }
    return lambda(`local*val`)
}
