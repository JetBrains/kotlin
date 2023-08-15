// !LANGUAGE: +ContextReceivers
// MODULE: callables_library

package callables.test

fun regularFun(): String = ""
fun regularFun(p1: Number): String = ""
fun regularFun(p1: Int): String = ""
fun regularFun(p1: Int, p2: Long): String = ""
fun regularFun(p1: Number, p2: Long): String = ""
fun regularFun(p1: Int, p2: Number): String = ""
fun regularFun(p1: Number, p2: Number): String = ""
fun Int.regularFun(): String = ""
fun Long.regularFun(): String = ""
fun Number.regularFun(): String = ""

context(Int) fun regularFun(): String = ""
context(Int) fun regularFun(p1: Number): String = ""
context(Int) fun regularFun(p1: Int): String = ""
context(Int) fun regularFun(p1: Int, p2: Long): String = ""
context(Int) fun regularFun(p1: Number, p2: Long): String = ""
context(Int) fun regularFun(p1: Int, p2: Number): String = ""
context(Int) fun regularFun(p1: Number, p2: Number): String = ""
context(Int) fun Int.regularFun(): String = ""
context(Int) fun Long.regularFun(): String = ""
context(Int) fun Number.regularFun(): String = ""

context(Int, Long) fun regularFun(): String = ""
context(Int, Long) fun regularFun(p1: Number): String = ""
context(Int, Long) fun regularFun(p1: Int): String = ""
context(Int, Long) fun regularFun(p1: Int, p2: Long): String = ""
context(Int, Long) fun regularFun(p1: Number, p2: Long): String = ""
context(Int, Long) fun regularFun(p1: Int, p2: Number): String = ""
context(Int, Long) fun regularFun(p1: Number, p2: Number): String = ""
context(Int, Long) fun Int.regularFun(): String = ""
context(Int, Long) fun Long.regularFun(): String = ""
context(Int, Long) fun Number.regularFun(): String = ""

class FunctionContainer {
    fun regularFun(): String = ""
    fun regularFun(p1: Number): String = ""
    fun regularFun(p1: Int): String = ""
    fun regularFun(p1: Int, p2: Long): String = ""
    fun regularFun(p1: Number, p2: Long): String = ""
    fun regularFun(p1: Int, p2: Number): String = ""
    fun regularFun(p1: Number, p2: Number): String = ""
    fun Int.regularFun(): String = ""
    fun Long.regularFun(): String = ""
    fun Number.regularFun(): String = ""

    context(Int) fun regularFun(): String = ""
    context(Int) fun regularFun(p1: Number): String = ""
    context(Int) fun regularFun(p1: Int): String = ""
    context(Int) fun regularFun(p1: Int, p2: Long): String = ""
    context(Int) fun regularFun(p1: Number, p2: Long): String = ""
    context(Int) fun regularFun(p1: Int, p2: Number): String = ""
    context(Int) fun regularFun(p1: Number, p2: Number): String = ""
    context(Int) fun Int.regularFun(): String = ""
    context(Int) fun Long.regularFun(): String = ""
    context(Int) fun Number.regularFun(): String = ""

    context(Int, Long) fun regularFun(): String = ""
    context(Int, Long) fun regularFun(p1: Number): String = ""
    context(Int, Long) fun regularFun(p1: Int): String = ""
    context(Int, Long) fun regularFun(p1: Int, p2: Long): String = ""
    context(Int, Long) fun regularFun(p1: Number, p2: Long): String = ""
    context(Int, Long) fun regularFun(p1: Int, p2: Number): String = ""
    context(Int, Long) fun regularFun(p1: Number, p2: Number): String = ""
    context(Int, Long) fun Int.regularFun(): String = ""
    context(Int, Long) fun Long.regularFun(): String = ""
    context(Int, Long) fun Number.regularFun(): String = ""
}

suspend fun suspendFun(value: Int, block: (Int) -> String): String = ""
suspend fun suspendFun(value: Int, block: suspend (Int) -> String): String = ""

inline fun inlineFun(block: (Int) -> String): String = ""
inline fun inlineFun(block: (Long) -> String): String = ""
inline fun inlineFun(block: (Number) -> String): String = ""

fun varargFun(vararg a: Int): String = ""
fun varargFun(a: Array<Int>): String = ""
fun varargFun(a: Array<out Int>): String = ""
fun varargFun(a: Array<in Int>): String = ""

fun varargFun(vararg a: String): String = ""
fun varargFun(a: Array<String>): String = ""
fun varargFun(a: Array<out String>): String = ""
fun varargFun(a: Array<in String>): String = ""

fun typeParameterFun(a: String): String = ""
fun <T> typeParameterFun(a: String): String = ""
fun <T> typeParameterFun(a: T): String = ""
fun <T : Any> typeParameterFun(a: T): String = ""
fun <T : CharSequence> typeParameterFun(a: T): String = ""
fun <T> typeParameterFun(a: T): String where T : CharSequence, T : Appendable = ""

val regularVal: String get() = ""
val Int.regularVal: String get() = ""
val Long.regularVal: String get() = ""
val Number.regularVal: String get() = ""

var regularVar: String get() = ""
    set(_) = Unit
var Int.regularVar: String get() = ""
    set(_) = Unit
var Long.regularVar: String get() = ""
    set(_) = Unit
var Number.regularVar: String get() = ""
    set(_) = Unit

val typeParameterVal: String get() = ""
val <T> T.typeParameterVal: String where T : CharSequence, T : Appendable get() = ""

var typeParameterVar: String get() = ""
    set(_) = Unit
var <T> T.typeParameterVar: String where T : CharSequence, T : Appendable get() = ""
    set(_) = Unit

const val constProperty: String = ""

inline val inlineVal: String get() = ""
inline var inlineVar: String get() = ""
    set(_) = Unit

class PropertyContainer {
    val regularVal: String get() = ""
    val Int.regularVal: String get() = ""
    val Long.regularVal: String get() = ""
    val Number.regularVal: String get() = ""

    var regularVar: String get() = ""
        set(_) = Unit
    var Int.regularVar: String get() = ""
        set(_) = Unit
    var Long.regularVar: String get() = ""
        set(_) = Unit
    var Number.regularVar: String get() = ""
        set(_) = Unit

    val typeParameterVal: String get() = ""
    val <T> T.typeParameterVal: String where T : CharSequence, T : Appendable get() = ""

    var typeParameterVar: String get() = ""
        set(_) = Unit
    var <T> T.typeParameterVar: String where T : CharSequence, T : Appendable get() = ""
        set(_) = Unit

    inline val inlineVal: String get() = ""
    inline var inlineVar: String get() = ""
        set(_) = Unit
}
