// RUN_PIPELINE_TILL: CODEGEN
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// IGNORE_NON_REVERSED_RESOLVE: KT-62937
// IGNORE_REVERSED_RESOLVE: KT-62937
// IGNORE_PARTIAL_BODY_ANALYSIS: KT-62937
// ISSUE: KT-85639
// LANGUAGE: +ContextParameters

// ---------- Classifiers (not reported) ----------

class `<C`(`not<Leading`: Int)

interface `<I`

object `<O`

annotation class `<A`

enum class `<E` { ENTRY }

class Outer {
    class `<Nested`
    inner class `<Inner`
    companion object `<Companion`
}

// ---------- Enum entries ----------

enum class Entries {
    <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<startingEntry`,<!>
    `middle<Entry`,
    normalEntry;
}

// ---------- Functions ----------

fun <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<topLevelFun`<!>() {}

fun String.<!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<extensionFun`<!>() {}

private fun `<privateFun`() {}

fun `middle<Fun`() {}

fun `trailing<`() {}

fun `>notLessThan`() {}

fun ` <leadingSpace`() {}

class Members {
    fun <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<memberFun`<!>() {}
}

// ---------- Properties ----------

val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<topLevelVal`<!> = 1

var <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<topLevelVar`<!> = 1

val String.<!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<extensionProperty`<!>: Int
    get() = 1

private val `<privateVal` = 1

val `middle<Val` = 1

object Constants {
    const val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<const`<!> = 1
}

class PropertyMembers {
    val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<memberVal`<!> = 1
    var <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<memberVar`<!> = 1
        set(<!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<newValue`<!>) {
            field = `<newValue`
        }
}

// ---------- Overrides ----------

interface Base {
    fun <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<baseFun`<!>()
    val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<baseVal`<!>: Int
}

class Derived : Base {
    override fun <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<baseFun`<!>() {}
    override val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<baseVal`<!>: Int = 1
}

// Fake overrides are not reported.
class DerivedFromDerived : Base by Derived()

// ---------- Type aliases ----------

typealias <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<Alias`<!> = String

typealias <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<GenericAlias`<!><T> = List<T>

typealias `middle<Alias` = String

// ---------- Declarations visible outside the module through other means ----------

@PublishedApi
internal fun <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<publishedApiFun`<!>() {}

@PublishedApi
internal val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<publishedApiVal`<!> = 1

open class OpenMembers {
    protected fun <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<protectedFun`<!>() {}
    protected val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<protectedVal`<!> = 1
}

// ---------- Private and internal declarations (not reported) ----------

internal fun `<internalFun`() {}

internal var `<internalVar` = 1

private typealias `<PrivateAlias` = String

internal typealias `<InternalAlias` = String

class NonPublicMembers {
    private fun `<privateMemberFun`() {}
    internal fun `<internalMemberFun`() {}
    private val `<privateMemberVal` = 1
    internal var `<internalMemberVar` = 1
}

// ---------- Members of private and internal classes (not reported) ----------

private class PrivateClass {
    fun `<memberFun`() {}
    val `<memberVal` = 1

    class Nested {
        fun `<nestedMemberFun`() {}
    }
}

internal class InternalClass {
    fun `<memberFun`() {}
    var `<memberVar` = 1
}

private object PrivateObject {
    const val `<const` = 1
}

class WithPrivateCompanion {
    private companion object {
        fun `<companionFun`() {}
    }
}

internal open class InternalOpenClass {
    protected fun `<protectedFun`() {}
}

// ---------- Value parameters ----------

fun params(<!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<param`: Int<!>, `middle<Param`: Int, <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>vararg `<vararg`: String<!>) {}

fun defaultParam(<!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<default`: Int = 1<!>) {}

class CtorParams(<!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<ctorParam`: Int<!>, <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>val <!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<ctorProperty`<!>: Int<!>)

context(<!LEADING_LESS_THAN_IN_DECLARATION_NAME!>`<context`: String<!>)
fun contextParam() {}

// ---------- Type parameters (not reported) ----------

fun <`<T`> genericFun(x: `<T`) {}

class GenericClass<`<T`>

inline fun <reified `<R`> reifiedFun() {}

// ---------- Local declarations (not reported) ----------

fun local() {
    val `<localVal` = 1
    var `<localVar` = 2
    fun `<localFun`(`<localFunParam`: Int) {}
    class `<LocalClass`(`<localCtorParam`: Int) {
        fun `<localClassMember`() {}
        val `<localClassProperty` = 1
    }

    val (`<d1`, `<d2`) = Pair(1, 2)

    for (`<loopVar` in 1..2) {}

    try {} catch (`<e`: Throwable) {}

    val lambda = { `<lambdaParam`: Int -> `<lambdaParam` }
    val anonymous = fun(`<anonParam`: Int) = `<anonParam`

    val anonymousObject = object {
        val `<objMember` = 1
        fun `<objFun`() {}
    }

    val `<delegated` by lazy { 1 }
}
