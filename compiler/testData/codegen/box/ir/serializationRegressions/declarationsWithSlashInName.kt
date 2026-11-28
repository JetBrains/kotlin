// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// WITH_STDLIB
// A slash in a class name breaks klib linkage: ClassId uses the slash as a package separator, so such classes
// cannot be resolved from dependent modules. They are reported by SLASH_IN_DECLARATION_NAME.
// Private and internal classes, and classes nested in them, may have slashes: dependent modules can't refer to them
// by name, even when public declarations use them, e.g. as wrappers or through class and property delegation.
// Functions, properties and enum entries with slashes link fine, but the KLIB ABI reader cannot read their
// qualified names, which breaks the ABI dump made by the test infrastructure, so they aren't tested here.
// Local declarations, type aliases, type and value parameters may have slashes.
// JsAllowInvalidCharsIdentifiersEscaping lets top-level declarations use characters invalid in JS identifiers.

// MODULE: lib
// FILE: lib.kt
package lib

import kotlin.reflect.KProperty

// Top-level declarations with characters invalid in JS identifiers, other than a slash
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
}

typealias ` /Alias/ ` = `Space Class`

// Private and internal classes with slashes, used by public declarations
private class `Internal/With/Slash` {
    fun render(): String = "Internal/With/Slash"
}

class InternalWithSlashDelegate {
    fun render(): String = `Internal/With/Slash`().render()
}

interface Renderer {
    fun render(): String
}

internal class `Internal/class` {
    fun render(): String = "Internal/class"
}

private object `Private/object` {
    val value: String = "Private/object"
}

internal enum class `Internal/enum` {
    `entry/one`,
}

private data class `Private/data`(val value: String)

private class `Private/Renderer` : Renderer {
    override fun render(): String = "Private/Renderer"
}

internal class `Internal/Renderer` : Renderer {
    override fun render(): String = "Internal/Renderer"
}

fun renderInternalClass(): String = `Internal/class`().render()

val privateObjectValue: String
    get() = `Private/object`.value

fun internalEnumEntryName(): String = `Internal/enum`.`entry/one`.name

fun privateDataCopy(): String = `Private/data`("a").copy(value = "b").value

fun privateRenderer(): Renderer = `Private/Renderer`()

fun internalRenderer(): Renderer = `Internal/Renderer`()

fun isPrivateRenderer(renderer: Renderer): Boolean = renderer is `Private/Renderer`

// Private inline functions are inlined before serialization
private inline fun inlineRender(): String = `Private/Renderer`().render()

fun renderThroughPrivateInline(): String = inlineRender()

// Private and internal classes nested in a public class
class SlashHost {
    private class `Private/nested` {
        fun render(): String = "Private/nested"
    }

    internal class `Internal/nested` {
        fun render(): String = "Internal/nested"
    }

    private companion object `Private/companion` {
        fun render(): String = "Private/companion"
    }

    fun render(): String = `Private/nested`().render() + `Internal/nested`().render() + `Private/companion`.render()
}

// Public classes nested in private and internal classes are effectively private or internal
private class PrivateOuter {
    class `Public/in/private` {
        fun render(): String = "Public/in/private"
    }

    class Middle {
        class `Deeply/nested` {
            fun render(): String = "Deeply/nested"
        }
    }
}

internal class InternalOuter {
    class `Public/in/internal` {
        fun render(): String = "Public/in/internal"
    }
}

fun renderNestedInNonPublic(): String =
    PrivateOuter.`Public/in/private`().render() +
            PrivateOuter.Middle.`Deeply/nested`().render() +
            InternalOuter.`Public/in/internal`().render()

// Class delegation to private and internal implementations
class PrivateDelegatingRenderer : Renderer by `Private/Renderer`()

class InternalDelegatingRenderer : Renderer by `Internal/Renderer`()

// Property delegates implemented by private and internal classes
private class `Private/Delegate` {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "Private/Delegate"
}

internal class `Internal/Delegate`(private var value: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) {
        value = newValue
    }
}

private class `Private/Provider` {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): `Private/Delegate` = `Private/Delegate`()
}

val privateDelegated: String by `Private/Delegate`()

var internalDelegated: String by `Internal/Delegate`("initial")

val providedDelegated: String by `Private/Provider`()

val lazyDelegated: String by lazy { `Private/Renderer`().render() }

class DelegatedMembers {
    val member: String by `Private/Delegate`()

    var memberVar: String by `Internal/Delegate`("member")
}

// Type aliases
class Real(val v: String)

typealias `Real/alias` = Real

typealias `List/alias`<T> = List<T>

fun makeAlias(v: String): `Real/alias` = Real(v)

fun aliasValue(x: `Real/alias`): String = x.v

fun makeListAlias(): `List/alias`<String> = listOf("a", "b")

// Type parameters
fun <`T/p`> identity(x: `T/p`): `T/p` = x

class Container<`T/p`>(val value: `T/p`) {
    fun <`R/r`> map(f: (`T/p`) -> `R/r`): Container<`R/r`> = Container(f(value))
}

inline fun <reified `T/reified`> isInstance(x: Any): Boolean = x is `T/reified`

// Value parameters
fun concat(`first/param`: String, `second/param`: String = "default"): String = `first/param` + `second/param`

class WithParams(`first/ctor/param`: String, `second/ctor/param`: String) {
    val combined: String = `first/ctor/param` + `second/ctor/param`
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    // Top-level declarations with characters invalid in JS identifiers, other than a slash
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
    if (`Enum-Class`.entries.map { it.name } != listOf("entry one")) return "Fail enum: ${`Enum-Class`.entries}"
    val aliased: ` /Alias/ ` = ` /Alias/ `("alias")
    if (aliased.value != "alias") return "Fail type alias with slashes"

    // Private and internal classes with slashes, used by public declarations
    if (InternalWithSlashDelegate().render() != "Internal/With/Slash") return "Fail internal with slash"
    if (renderInternalClass() != "Internal/class") return "Fail internal class: ${renderInternalClass()}"
    if (privateObjectValue != "Private/object") return "Fail private object: $privateObjectValue"
    if (internalEnumEntryName() != "entry/one") return "Fail internal enum: ${internalEnumEntryName()}"
    if (privateDataCopy() != "b") return "Fail private data class: ${privateDataCopy()}"
    if (privateRenderer().render() != "Private/Renderer") return "Fail private implementation: ${privateRenderer().render()}"
    if (internalRenderer().render() != "Internal/Renderer") return "Fail internal implementation: ${internalRenderer().render()}"
    if (!isPrivateRenderer(privateRenderer())) return "Fail type check of a private class"
    if (isPrivateRenderer(internalRenderer())) return "Fail negative type check of a private class"
    if (renderThroughPrivateInline() != "Private/Renderer") return "Fail private inline function: ${renderThroughPrivateInline()}"
    if (SlashHost().render() != "Private/nestedInternal/nestedPrivate/companion") return "Fail nested classes: ${SlashHost().render()}"
    if (renderNestedInNonPublic() != "Public/in/privateDeeply/nestedPublic/in/internal") {
        return "Fail classes nested in non-public classes: ${renderNestedInNonPublic()}"
    }
    if (PrivateDelegatingRenderer().render() != "Private/Renderer") return "Fail class delegation to a private class"
    if (InternalDelegatingRenderer().render() != "Internal/Renderer") return "Fail class delegation to an internal class"
    if (privateDelegated != "Private/Delegate") return "Fail private property delegate: $privateDelegated"
    if (internalDelegated != "initial") return "Fail internal property delegate: $internalDelegated"
    internalDelegated = "updated"
    if (internalDelegated != "updated") return "Fail internal property delegate after write: $internalDelegated"
    if (providedDelegated != "Private/Delegate") return "Fail private delegate provider: $providedDelegated"
    if (lazyDelegated != "Private/Renderer") return "Fail lazy delegate: $lazyDelegated"
    val delegatedMembers = DelegatedMembers()
    if (delegatedMembers.member != "Private/Delegate") return "Fail private member delegate: ${delegatedMembers.member}"
    if (delegatedMembers.memberVar != "member") return "Fail internal member delegate: ${delegatedMembers.memberVar}"
    delegatedMembers.memberVar = "updated"
    if (delegatedMembers.memberVar != "updated") return "Fail internal member delegate after write: ${delegatedMembers.memberVar}"

    // Type aliases
    val alias: `Real/alias` = `Real/alias`("alias")
    if (alias.v != "alias") return "Fail type alias: ${alias.v}"
    val list: `List/alias`<Int> = listOf(1, 2)
    if (list.size != 2) return "Fail generic type alias: $list"
    if (aliasValue(makeAlias("sig")) != "sig") return "Fail type alias in signatures"
    if (makeListAlias() != listOf("a", "b")) return "Fail generic type alias in signatures: ${makeListAlias()}"

    // Type parameters
    if (identity("id") != "id") return "Fail generic function"
    if (Container(21).map { it * 2 }.value != 42) return "Fail generic class"
    if (!isInstance<String>("s") || isInstance<Int>("s")) return "Fail reified type parameter"

    // Value parameters
    if (concat(`second/param` = "B", `first/param` = "A") != "AB") return "Fail named value parameters"
    if (concat("A") != "Adefault") return "Fail default value parameter"
    val withParams = WithParams(`second/ctor/param` = "2", `first/ctor/param` = "1")
    if (withParams.combined != "12") return "Fail constructor parameters: ${withParams.combined}"

    // Local declarations
    fun `local/function`(): String = "local"
    class `Local/class`(val value: String)
    if (`Local/class`(`local/function`()).value != "local") return "Fail local class and function"
    val (`destructured/first`, `destructured/second`) = Pair(1, 2)
    if (`destructured/first` + `destructured/second` != 3) return "Fail destructuring"
    val `local/val` = "O"
    val lambda = { `lambda/param`: String -> `lambda/param` + "K" }
    return lambda(`local/val`)
}
