// RUN_PIPELINE_TILL: FIR2IR
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE
// IGNORE_NON_REVERSED_RESOLVE: KT-62937
// IGNORE_REVERSED_RESOLVE: KT-62937
// IGNORE_PARTIAL_BODY_ANALYSIS: KT-62937
// ISSUE: KT-85639
// LANGUAGE: +ContextParameters

// ---------- Classifiers ----------

class <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`C.c`<!>(val `p.p`: String, `q.q`: Int)

interface <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`I.i`<!>

object <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`O.o`<!>

annotation class <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`A.a`<!>

data class <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`D.d`<!>(val `component.one`: Int)

enum class <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`E.e`<!> {
    `ENTRY.ONE`,
    `ENTRY.TWO`;
}

typealias `T.t` = String

class Outer {
    class `Nested.n`
    inner class `Inner.i`
    companion object `Companion.c`

    val `member.property` = 1
    var `member.var` = 1
        set(`new.value`) {
            field = `new.value`
        }

    fun `member.function`() {}
}

// ---------- Callables ----------

fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`F.f`<!>() {}

fun String.<!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`ext.fun`<!>() {}

val <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`top.property`<!> = 1

val String.<!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`ext.property`<!>: Int
    get() = 1

fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`with.params`<!>(`a.a`: Int, vararg `b.b`: String) {}

private fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`private.fun`<!>() {}

private val <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`private.property`<!> = 1

private class <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`Private.class`<!>

// ---------- Type parameters ----------

fun <`T.fun`> genericFun(x: `T.fun`) {}

class GenericClass<`T.class`>

interface GenericInterface<in `T.in`, out `T.out`>

typealias GenericAlias<`T.alias`> = List<`T.alias`>

fun <`T.bound` : Comparable<`T.bound`>> boundedFun(x: `T.bound`) {}

fun <`T.where`> whereFun(x: `T.where`) where `T.where` : CharSequence, `T.where` : Comparable<`T.where`> {}

inline fun <reified `T.reified`> reifiedFun() {}

val <`T.prop`> `T.prop`.genericExtProperty: Int
    get() = 1

class GenericMembers<`T.outer`> {
    fun <`T.member`> memberFun(x: `T.member`, y: `T.outer`) {}
}

fun <NormalTypeParameter> normalTypeParameter(x: NormalTypeParameter) {}

fun localGeneric() {
    fun <`T.local`> localFun(x: `T.local`) {}
}

context(`c.c`: String)
fun contextParam() {}

// ---------- Leading and trailing dots ----------

fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>` .leadingDotFun`<!>() {}

fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`.noSpaceLeadingDotFun`<!>() {}

class <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`   .LeadingDotClass `<!>

val <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>` .leadingDotProperty`<!> = 1

fun <` .LeadingDotTypeParameter`> leadingDotTypeParameter() {}

fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>`trailingDot.`<!>() {}

val <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>` .bothEnds. `<!> = 1

// ---------- Local declarations ----------

fun local() {
    val `local.val` = 1
    var `local.var` = 2
    fun `local.fun`() {}
    class `Local.class`

    val (`d.a`, `d.b`) = Pair(1, 2)

    for (`loop.var` in 1..2) {}

    try {} catch (`e.e`: Throwable) {}

    val lambda = { `lambda.param`: Int -> `lambda.param` }
    val anonymous = fun(`anon.param`: Int) = `anon.param`

    `loop.label`@ for (i in 1..2) {
        break@`loop.label`
    }

    val anonymousObject = object {
        val `obj.member` = 1
    }
}
