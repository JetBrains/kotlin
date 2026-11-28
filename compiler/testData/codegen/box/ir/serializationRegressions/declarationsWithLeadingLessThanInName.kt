// ISSUE: KT-85639
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// WITH_STDLIB
// Private and internal declarations, and declarations in private and internal classes, may start with `<`:
// dependent modules can't refer to them by name, even when public declarations use them, e.g. as wrappers
// or through class and property delegation.

// MODULE: lib
// FILE: lib.kt
package lib

import kotlin.reflect.KProperty

// IrClass: classes
class `<startingClass`(val value: String)
class `middle<Class`(val value: String)
class normalClass(val value: String)

// IrClass: interfaces
interface `<startingInterface` {
    fun get(): String
}
interface `middle<Interface` {
    fun get(): String
}
interface normalInterface {
    fun get(): String
}

class InterfacesImpl : `<startingInterface`, `middle<Interface`, normalInterface {
    override fun get(): String = "impl"
}

// IrClass: objects
object `<startingObject` {
    val value: String = "<startingObject"
}
object `middle<Object` {
    val value: String = "middle<Object"
}
object normalObject {
    val value: String = "normalObject"
}

// IrClass: annotation classes
annotation class `<startingAnnotation`
annotation class `middle<Annotation`
annotation class normalAnnotation

@`<startingAnnotation`
@`middle<Annotation`
@normalAnnotation
fun annotated(): String = "annotated"

// IrClass: enum classes, IrEnumEntry: enum entries
enum class `<startingEnum` { A }
enum class `middle<Enum` { A }
enum class normalEnum {
    `middle<Entry`,
    normalEntry,
}

// IrClass: nested, inner classes and companion objects
class OuterStarting {
    class `<startingNested` {
        val value: String = "nested"
    }

    inner class `<startingInner` {
        val value: String = "inner"
    }

    companion object `<startingCompanion` {
        val value: String = "companion"
    }
}

class OuterMiddle {
    class `middle<Nested` {
        val value: String = "nested"
    }

    inner class `middle<Inner` {
        val value: String = "inner"
    }

    companion object `middle<Companion` {
        val value: String = "companion"
    }
}

class OuterNormal {
    class normalNested {
        val value: String = "nested"
    }

    inner class normalInner {
        val value: String = "inner"
    }

    companion object normalCompanion {
        val value: String = "companion"
    }
}

// IrSimpleFunction: top-level functions
fun `middle<Function`(): String = "middle<Function"
fun normalFunction(): String = "normalFunction"

// IrSimpleFunction: extension functions
fun String.`middle<ExtensionFunction`(): String = this + "m"
fun String.normalExtensionFunction(): String = this + "n"

// IrProperty: top-level properties
val `middle<Val`: String = "middle<Val"
val normalVal: String = "normalVal"

var `middle<Var`: String = ""
var normalVar: String = ""

// IrProperty: extension properties
val String.`middle<ExtensionProperty`: String
    get() = this + "m"
val String.normalExtensionProperty: String
    get() = this + "n"

// IrSimpleFunction, IrProperty: members
class Members {
    fun `middle<MemberFunction`(): String = "middle<MemberFunction"
    fun normalMemberFunction(): String = "normalMemberFunction"

    val `middle<MemberVal`: String = "middle<MemberVal"
    val normalMemberVal: String = "normalMemberVal"

    var `middle<MemberVar`: String = ""
    var normalMemberVar: String = ""
}

object Constants {
    const val `middle<Const`: String = "middle<Const"
    const val normalConst: String = "normalConst"
}

// IrTypeAlias
typealias `middle<Alias` = normalClass
typealias normalAlias = normalClass

typealias `middle<GenericAlias`<T> = List<T>
typealias normalGenericAlias<T> = List<T>

// IrTypeParameter
fun <`<startingT`, `middle<T`, normalT> triple(a: `<startingT`, b: `middle<T`, c: normalT): Triple<`<startingT`, `middle<T`, normalT> =
    Triple(a, b, c)

class Generic<`<startingT`, `middle<T`, normalT>(val a: `<startingT`, val b: `middle<T`, val c: normalT)

inline fun <reified `<startingR`> isStarting(x: Any): Boolean = x is `<startingR`
inline fun <reified `middle<R`> isMiddle(x: Any): Boolean = x is `middle<R`
inline fun <reified normalR> isNormal(x: Any): Boolean = x is normalR

// IrValueParameter
fun concat(`middle<Param`: String, normalParam: String = "n"): String =
    `middle<Param` + normalParam

class WithParams(`middle<CtorParam`: String, normalCtorParam: String) {
    val combined: String = `middle<CtorParam` + normalCtorParam
}

// Private and internal declarations starting with `<`, used by public declarations
private fun `<privateFunction`(): String = "<privateFunction"
internal fun `<internalFunction`(): String = "<internalFunction"

private val `<privateVal`: String = "<privateVal"
internal var `<internalVar`: String = "initial"

private fun String.`<privateExtension`(): String = this + "<"
internal val String.`<internalExtensionProperty`: String
    get() = this + ">"

private typealias `<PrivateAlias` = normalClass
internal typealias `<InternalAlias` = List<String>

fun callPrivateAndInternalFunctions(): String = `<privateFunction`() + `<internalFunction`()

fun readPrivateAndInternalProperties(): String = `<privateVal` + `<internalVar`

fun writeInternalVar(value: String) {
    `<internalVar` = value
}

fun callPrivateAndInternalExtensions(): String = "a".`<privateExtension`() + "b".`<internalExtensionProperty`

fun usePrivateAndInternalAliases(): String {
    val alias: `<PrivateAlias` = `<PrivateAlias`("alias")
    val list: `<InternalAlias` = listOf("list")
    return alias.value + list.single()
}

// Private inline functions are inlined before serialization
private inline fun `<privateInline`(block: () -> String): String = "<" + block()

fun callPrivateInline(): String = `<privateInline` { "inline" }

// Private and internal members of a public class
class LessThanHost {
    private fun `<privateMember`(): String = "a"
    internal fun `<internalMember`(): String = "b"
    private val `<privateMemberVal`: String = "c"
    internal var `<internalMemberVar`: String = "d"

    private companion object {
        fun `<companionMember`(): String = "e"
    }

    fun render(): String =
        `<privateMember`() + `<internalMember`() + `<privateMemberVal` + `<internalMemberVar` + `<companionMember`()
}

// Public members of private and internal classes are effectively private or internal
private class PrivateLessThan {
    fun `<memberInPrivate`(): String = "f"
    val `<valInPrivate`: String = "g"

    class Nested {
        fun `<memberInNested`(): String = "h"
    }
}

internal class InternalLessThan {
    fun `<memberInInternal`(): String = "i"
    var `<varInInternal`: String = "j"
}

private object PrivateLessThanObject {
    const val `<constInPrivate`: String = "k"
}

fun renderMembersOfNonPublicClasses(): String {
    val internalLessThan = InternalLessThan()
    internalLessThan.`<varInInternal` = "J"
    return PrivateLessThan().`<memberInPrivate`() +
            PrivateLessThan().`<valInPrivate` +
            PrivateLessThan.Nested().`<memberInNested`() +
            internalLessThan.`<memberInInternal`() +
            internalLessThan.`<varInInternal` +
            PrivateLessThanObject.`<constInPrivate`
}

// A private implementation of a public interface, with members starting with `<`
interface LessThanRenderer {
    fun render(): String
}

private class PrivateLessThanRenderer : LessThanRenderer {
    fun `<render`(): String = "<render"
    override fun render(): String = `<render`()
}

fun privateLessThanRenderer(): LessThanRenderer = PrivateLessThanRenderer()

fun isPrivateLessThanRenderer(renderer: LessThanRenderer): Boolean = renderer is PrivateLessThanRenderer

// Class delegation to a private implementation
class DelegatingLessThanRenderer : LessThanRenderer by PrivateLessThanRenderer()

// Property delegates using private and internal declarations starting with `<`
private val `<privateLazy`: String by lazy { "<privateLazy" }

val readPrivateLazy: String
    get() = `<privateLazy`

val lazyDelegated: String by lazy { `<privateFunction`() }

internal var `<internalDelegateTarget`: String = "target"

val delegatedToPrivateProperty: String by ::`<privateVal`

var delegatedToInternalProperty: String by ::`<internalDelegateTarget`

internal class LessThanDelegate(private var value: String) {
    private fun `<prefixed`(name: String): String = "<$name=$value"

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = `<prefixed`(property.name)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) {
        value = newValue
    }
}

private class LessThanProvider {
    fun `<create`(): LessThanDelegate = LessThanDelegate("provided")

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): LessThanDelegate = `<create`()
}

var delegatedByInternalClass: String by LessThanDelegate("initial")

val providedByPrivateClass: String by LessThanProvider()

class DelegatedLessThanMembers {
    private val `<privateMember`: String = "<privateMember"
    private val `<privateLazyMember`: String by lazy { "<privateLazyMember" }

    val delegatedToPrivateMember: String by this::`<privateMember`

    val lazyMember: String
        get() = `<privateLazyMember`

    var delegatedByInternalClass: String by LessThanDelegate("member")
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    // IrClass: classes
    if (`<startingClass`("s").value != "s") return "Fail <startingClass"
    if (`middle<Class`("m").value != "m") return "Fail middle<Class"
    if (normalClass("n").value != "n") return "Fail normalClass"

    // IrClass: interfaces
    val impl = InterfacesImpl()
    if ((impl as `<startingInterface`).get() != "impl") return "Fail <startingInterface"
    if ((impl as `middle<Interface`).get() != "impl") return "Fail middle<Interface"
    if ((impl as normalInterface).get() != "impl") return "Fail normalInterface"

    // IrClass: objects
    if (`<startingObject`.value != "<startingObject") return "Fail <startingObject"
    if (`middle<Object`.value != "middle<Object") return "Fail middle<Object"
    if (normalObject.value != "normalObject") return "Fail normalObject"

    // IrClass: annotation classes
    if (annotated() != "annotated") return "Fail annotation classes"

    // IrClass: enum classes, IrEnumEntry: enum entries
    if (`<startingEnum`.A.name != "A") return "Fail <startingEnum"
    if (`middle<Enum`.A.name != "A") return "Fail middle<Enum"
    if (normalEnum.entries.map { it.name } != listOf("middle<Entry", "normalEntry")) return "Fail enum entries: ${normalEnum.entries}"
    if (normalEnum.valueOf("middle<Entry") != normalEnum.`middle<Entry`) return "Fail middle<Entry"
    if (normalEnum.valueOf("normalEntry") != normalEnum.normalEntry) return "Fail normalEntry"
    val entry: normalEnum = normalEnum.`middle<Entry`
    val whenResult = when (entry) {
        normalEnum.`middle<Entry` -> "m"
        normalEnum.normalEntry -> "n"
    }
    if (whenResult != "m") return "Fail enum when: $whenResult"

    // IrClass: nested, inner classes and companion objects
    if (OuterStarting.`<startingNested`().value != "nested") return "Fail <startingNested"
    if (OuterStarting().`<startingInner`().value != "inner") return "Fail <startingInner"
    if (OuterStarting.value != "companion") return "Fail <startingCompanion"
    if (OuterStarting.`<startingCompanion`.value != "companion") return "Fail <startingCompanion by name"
    if (OuterMiddle.`middle<Nested`().value != "nested") return "Fail middle<Nested"
    if (OuterMiddle().`middle<Inner`().value != "inner") return "Fail middle<Inner"
    if (OuterMiddle.value != "companion") return "Fail middle<Companion"
    if (OuterMiddle.`middle<Companion`.value != "companion") return "Fail middle<Companion by name"
    if (OuterNormal.normalNested().value != "nested") return "Fail normalNested"
    if (OuterNormal().normalInner().value != "inner") return "Fail normalInner"
    if (OuterNormal.value != "companion") return "Fail normalCompanion"
    if (OuterNormal.normalCompanion.value != "companion") return "Fail normalCompanion by name"

    // IrSimpleFunction: top-level functions
    if (`middle<Function`() != "middle<Function") return "Fail middle<Function"
    if (normalFunction() != "normalFunction") return "Fail normalFunction"

    // IrSimpleFunction: extension functions
    if ("a".`middle<ExtensionFunction`() != "am") return "Fail middle<ExtensionFunction"
    if ("a".normalExtensionFunction() != "an") return "Fail normalExtensionFunction"

    // IrProperty: top-level properties
    if (`middle<Val` != "middle<Val") return "Fail middle<Val"
    if (normalVal != "normalVal") return "Fail normalVal"
    `middle<Var` = "m"
    normalVar = "n"
    if (`middle<Var` != "m") return "Fail middle<Var: ${`middle<Var`}"
    if (normalVar != "n") return "Fail normalVar: $normalVar"

    // IrProperty: extension properties
    if ("a".`middle<ExtensionProperty` != "am") return "Fail middle<ExtensionProperty"
    if ("a".normalExtensionProperty != "an") return "Fail normalExtensionProperty"

    // IrSimpleFunction, IrProperty: members
    val members = Members()
    if (members.`middle<MemberFunction`() != "middle<MemberFunction") return "Fail middle<MemberFunction"
    if (members.normalMemberFunction() != "normalMemberFunction") return "Fail normalMemberFunction"
    if (members.`middle<MemberVal` != "middle<MemberVal") return "Fail middle<MemberVal"
    if (members.normalMemberVal != "normalMemberVal") return "Fail normalMemberVal"
    members.`middle<MemberVar` = "m"
    members.normalMemberVar = "n"
    if (members.`middle<MemberVar` != "m") return "Fail middle<MemberVar"
    if (members.normalMemberVar != "n") return "Fail normalMemberVar"
    if (Constants.`middle<Const` != "middle<Const") return "Fail middle<Const"
    if (Constants.normalConst != "normalConst") return "Fail normalConst"

    // IrTypeAlias
    val middleAlias: `middle<Alias` = `middle<Alias`("m")
    val normalAliasValue: normalAlias = normalAlias("n")
    if (middleAlias.value + normalAliasValue.value != "mn") return "Fail type aliases"
    val middleList: `middle<GenericAlias`<Int> = listOf(2)
    val normalList: normalGenericAlias<Int> = listOf(3)
    if (middleList + normalList != listOf(2, 3)) return "Fail generic type aliases"

    // IrTypeParameter
    if (triple("s", 1, 'n') != Triple("s", 1, 'n')) return "Fail type parameters of function"
    val generic = Generic("s", 1, 'n')
    if (generic.a != "s" || generic.b != 1 || generic.c != 'n') return "Fail type parameters of class"
    if (!isStarting<String>("s") || isStarting<Int>("s")) return "Fail reified <startingR"
    if (!isMiddle<String>("s") || isMiddle<Int>("s")) return "Fail reified middle<R"
    if (!isNormal<String>("s") || isNormal<Int>("s")) return "Fail reified normalR"

    // IrValueParameter
    if (concat(`middle<Param` = "m") != "mn") return "Fail value parameters"
    if (concat(normalParam = "N", `middle<Param` = "m") != "mN") return "Fail named value parameters"
    val withParams = WithParams(normalCtorParam = "n", `middle<CtorParam` = "m")
    if (withParams.combined != "mn") return "Fail constructor parameters: ${withParams.combined}"
    val lambda = { `<startingLambdaParam`: String, `middle<LambdaParam`: String, normalLambdaParam: String ->
        `<startingLambdaParam` + `middle<LambdaParam` + normalLambdaParam
    }
    if (lambda("s", "m", "n") != "smn") return "Fail lambda parameters"

    // Private and internal declarations starting with `<`, used by public declarations
    if (callPrivateAndInternalFunctions() != "<privateFunction<internalFunction") {
        return "Fail private and internal functions: ${callPrivateAndInternalFunctions()}"
    }
    if (readPrivateAndInternalProperties() != "<privateValinitial") {
        return "Fail private and internal properties: ${readPrivateAndInternalProperties()}"
    }
    writeInternalVar("updated")
    if (readPrivateAndInternalProperties() != "<privateValupdated") {
        return "Fail internal var after write: ${readPrivateAndInternalProperties()}"
    }
    if (callPrivateAndInternalExtensions() != "a<b>") return "Fail private and internal extensions: ${callPrivateAndInternalExtensions()}"
    if (usePrivateAndInternalAliases() != "aliaslist") return "Fail private and internal type aliases: ${usePrivateAndInternalAliases()}"
    if (callPrivateInline() != "<inline") return "Fail private inline function: ${callPrivateInline()}"
    if (LessThanHost().render() != "abcde") return "Fail private and internal members: ${LessThanHost().render()}"
    if (renderMembersOfNonPublicClasses() != "fghiJk") return "Fail members of non-public classes: ${renderMembersOfNonPublicClasses()}"
    if (privateLessThanRenderer().render() != "<render") return "Fail private implementation: ${privateLessThanRenderer().render()}"
    if (!isPrivateLessThanRenderer(privateLessThanRenderer())) return "Fail type check of a private class"
    if (isPrivateLessThanRenderer(DelegatingLessThanRenderer())) return "Fail negative type check of a private class"
    if (DelegatingLessThanRenderer().render() != "<render") return "Fail class delegation: ${DelegatingLessThanRenderer().render()}"
    if (readPrivateLazy != "<privateLazy") return "Fail private lazy property: $readPrivateLazy"
    if (lazyDelegated != "<privateFunction") return "Fail lazy delegate: $lazyDelegated"
    if (delegatedToPrivateProperty != "<privateVal") return "Fail delegation to a private property: $delegatedToPrivateProperty"
    if (delegatedToInternalProperty != "target") return "Fail delegation to an internal property: $delegatedToInternalProperty"
    delegatedToInternalProperty = "updated"
    if (delegatedToInternalProperty != "updated") {
        return "Fail delegation to an internal property after write: $delegatedToInternalProperty"
    }
    if (delegatedByInternalClass != "<delegatedByInternalClass=initial") {
        return "Fail internal property delegate: $delegatedByInternalClass"
    }
    delegatedByInternalClass = "updated"
    if (delegatedByInternalClass != "<delegatedByInternalClass=updated") {
        return "Fail internal property delegate after write: $delegatedByInternalClass"
    }
    if (providedByPrivateClass != "<providedByPrivateClass=provided") return "Fail private delegate provider: $providedByPrivateClass"
    val delegatedMembers = DelegatedLessThanMembers()
    if (delegatedMembers.delegatedToPrivateMember != "<privateMember") {
        return "Fail delegation to a private member: ${delegatedMembers.delegatedToPrivateMember}"
    }
    if (delegatedMembers.lazyMember != "<privateLazyMember") return "Fail private lazy member: ${delegatedMembers.lazyMember}"
    if (delegatedMembers.delegatedByInternalClass != "<delegatedByInternalClass=member") {
        return "Fail internal member delegate: ${delegatedMembers.delegatedByInternalClass}"
    }
    delegatedMembers.delegatedByInternalClass = "updated"
    if (delegatedMembers.delegatedByInternalClass != "<delegatedByInternalClass=updated") {
        return "Fail internal member delegate after write: ${delegatedMembers.delegatedByInternalClass}"
    }

    // Local IrClass and IrSimpleFunction
    class `<startingLocalClass`(val value: String)
    class `middle<LocalClass`(val value: String)
    class normalLocalClass(val value: String)
    fun `<startingLocalFunction`(): String = "s"
    fun `middle<LocalFunction`(): String = "m"
    fun normalLocalFunction(): String = "n"
    val localClasses = `<startingLocalClass`(`<startingLocalFunction`()).value +
            `middle<LocalClass`(`middle<LocalFunction`()).value +
            normalLocalClass(normalLocalFunction()).value
    if (localClasses != "smn") return "Fail local classes and functions: $localClasses"

    // IrVariable
    val `<startingLocalVal` = "s"
    val `middle<LocalVal` = "m"
    val normalLocalVal = "n"
    if (`<startingLocalVal` + `middle<LocalVal` + normalLocalVal != "smn") return "Fail local variables"
    val (`<startingDestructured`, `middle<Destructured`, normalDestructured) = Triple("s", "m", "n")
    if (`<startingDestructured` + `middle<Destructured` + normalDestructured != "smn") return "Fail destructuring"

    // IrLocalDelegatedProperty
    val `<startingDelegated` by lazy { "s" }
    val `middle<Delegated` by lazy { "m" }
    val normalDelegated by lazy { "n" }
    if (`<startingDelegated` + `middle<Delegated` + normalDelegated != "smn") return "Fail local delegated properties"
    
    return "OK"
} 
