// WITH_STDLIB
// IGNORE_BACKEND: ANDROID
// IGNORE_BACKEND: NATIVE

// FILE: 1.kt

package kotlin.jvm

annotation class JvmInline

// FILE: 2.kt

import kotlin.jvm.JvmInline
import kotlin.coroutines.*

@JvmInline
value class VCString(val a: String)
@JvmInline
value class VCStringNullable(val a: String?)
@JvmInline
value class VCAny(val a: Any)
@JvmInline
value class VCAnyNullable(val a: Any?)
@JvmInline
value class VCInt(val a: Int)
@JvmInline
value class VCIntNullable(val a: Int?)

var result: Any? = null
fun ordinaryNoninlineReturnsVCString(): VCString = VCString("OK")
fun ordinaryNoninlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
fun ordinaryNoninlineReturnsVCAny(): VCAny = VCAny("OK")
fun ordinaryNoninlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
fun ordinaryNoninlineReturnsVCInt(): VCInt = VCInt(42)
fun ordinaryNoninlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
fun ordinaryNoninlineReturnsVCString_Null(): VCString? = null
fun ordinaryNoninlineReturnsVCStringNullable_Null(): VCStringNullable? = null
fun ordinaryNoninlineReturnsVCAny_Null(): VCAny? = null
fun ordinaryNoninlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
fun ordinaryNoninlineReturnsVCInt_Null(): VCInt? = null
fun ordinaryNoninlineReturnsVCIntNullable_Null(): VCIntNullable? = null
fun ordinaryNoninlineAcceptsVCString(i: Int, vc: VCString) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCAny(i: Int, vc: VCAny) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCInt(i: Int, vc: VCInt) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCString_Null(i: Int, vc: VCString?) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
    result = vc
}

fun ordinaryNoninlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
    result = vc
}

inline fun ordinaryInlineReturnsVCString(): VCString = VCString("OK")
inline fun ordinaryInlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
inline fun ordinaryInlineReturnsVCAny(): VCAny = VCAny("OK")
inline fun ordinaryInlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
inline fun ordinaryInlineReturnsVCInt(): VCInt = VCInt(42)
inline fun ordinaryInlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
inline fun ordinaryInlineReturnsVCString_Null(): VCString? = null
inline fun ordinaryInlineReturnsVCStringNullable_Null(): VCStringNullable? = null
inline fun ordinaryInlineReturnsVCAny_Null(): VCAny? = null
inline fun ordinaryInlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
inline fun ordinaryInlineReturnsVCInt_Null(): VCInt? = null
inline fun ordinaryInlineReturnsVCIntNullable_Null(): VCIntNullable? = null
inline fun ordinaryInlineAcceptsVCString(i: Int, vc: VCString) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCAny(i: Int, vc: VCAny) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCInt(i: Int, vc: VCInt) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCString_Null(i: Int, vc: VCString?) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
    result = vc
}

inline fun ordinaryInlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
    result = vc
}

suspend fun suspendNoninlineReturnsVCString(): VCString = VCString("OK")
suspend fun suspendNoninlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
suspend fun suspendNoninlineReturnsVCAny(): VCAny = VCAny("OK")
suspend fun suspendNoninlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
suspend fun suspendNoninlineReturnsVCInt(): VCInt = VCInt(42)
suspend fun suspendNoninlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
suspend fun suspendNoninlineReturnsVCString_Null(): VCString? = null
suspend fun suspendNoninlineReturnsVCStringNullable_Null(): VCStringNullable? = null
suspend fun suspendNoninlineReturnsVCAny_Null(): VCAny? = null
suspend fun suspendNoninlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
suspend fun suspendNoninlineReturnsVCInt_Null(): VCInt? = null
suspend fun suspendNoninlineReturnsVCIntNullable_Null(): VCIntNullable? = null
suspend fun suspendNoninlineAcceptsVCString(i: Int, vc: VCString) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCAny(i: Int, vc: VCAny) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCInt(i: Int, vc: VCInt) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCString_Null(i: Int, vc: VCString?) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
    result = vc
}

suspend fun suspendNoninlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
    result = vc
}

suspend inline fun suspendInlineReturnsVCString(): VCString = VCString("OK")
suspend inline fun suspendInlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
suspend inline fun suspendInlineReturnsVCAny(): VCAny = VCAny("OK")
suspend inline fun suspendInlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
suspend inline fun suspendInlineReturnsVCInt(): VCInt = VCInt(42)
suspend inline fun suspendInlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
suspend inline fun suspendInlineReturnsVCString_Null(): VCString? = null
suspend inline fun suspendInlineReturnsVCStringNullable_Null(): VCStringNullable? = null
suspend inline fun suspendInlineReturnsVCAny_Null(): VCAny? = null
suspend inline fun suspendInlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
suspend inline fun suspendInlineReturnsVCInt_Null(): VCInt? = null
suspend inline fun suspendInlineReturnsVCIntNullable_Null(): VCIntNullable? = null
suspend inline fun suspendInlineAcceptsVCString(i: Int, vc: VCString) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCAny(i: Int, vc: VCAny) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCInt(i: Int, vc: VCInt) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCString_Null(i: Int, vc: VCString?) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
    result = vc
}

suspend inline fun suspendInlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
    result = vc
}

class C {
    fun ordinaryNoninlineReturnsVCString(): VCString = VCString("OK")
    fun ordinaryNoninlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
    fun ordinaryNoninlineReturnsVCAny(): VCAny = VCAny("OK")
    fun ordinaryNoninlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
    fun ordinaryNoninlineReturnsVCInt(): VCInt = VCInt(42)
    fun ordinaryNoninlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
    fun ordinaryNoninlineReturnsVCString_Null(): VCString? = null
    fun ordinaryNoninlineReturnsVCStringNullable_Null(): VCStringNullable? = null
    fun ordinaryNoninlineReturnsVCAny_Null(): VCAny? = null
    fun ordinaryNoninlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
    fun ordinaryNoninlineReturnsVCInt_Null(): VCInt? = null
    fun ordinaryNoninlineReturnsVCIntNullable_Null(): VCIntNullable? = null
    fun ordinaryNoninlineAcceptsVCString(i: Int, vc: VCString) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCAny(i: Int, vc: VCAny) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCInt(i: Int, vc: VCInt) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCString_Null(i: Int, vc: VCString?) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
        result = vc
    }

    fun ordinaryNoninlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
        result = vc
    }

    inline fun ordinaryInlineReturnsVCString(): VCString = VCString("OK")
    inline fun ordinaryInlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
    inline fun ordinaryInlineReturnsVCAny(): VCAny = VCAny("OK")
    inline fun ordinaryInlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
    inline fun ordinaryInlineReturnsVCInt(): VCInt = VCInt(42)
    inline fun ordinaryInlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
    inline fun ordinaryInlineReturnsVCString_Null(): VCString? = null
    inline fun ordinaryInlineReturnsVCStringNullable_Null(): VCStringNullable? = null
    inline fun ordinaryInlineReturnsVCAny_Null(): VCAny? = null
    inline fun ordinaryInlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
    inline fun ordinaryInlineReturnsVCInt_Null(): VCInt? = null
    inline fun ordinaryInlineReturnsVCIntNullable_Null(): VCIntNullable? = null
    inline fun ordinaryInlineAcceptsVCString(i: Int, vc: VCString) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCAny(i: Int, vc: VCAny) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCInt(i: Int, vc: VCInt) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCString_Null(i: Int, vc: VCString?) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
        result = vc
    }

    inline fun ordinaryInlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
        result = vc
    }

    suspend fun suspendNoninlineReturnsVCString(): VCString = VCString("OK")
    suspend fun suspendNoninlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
    suspend fun suspendNoninlineReturnsVCAny(): VCAny = VCAny("OK")
    suspend fun suspendNoninlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
    suspend fun suspendNoninlineReturnsVCInt(): VCInt = VCInt(42)
    suspend fun suspendNoninlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
    suspend fun suspendNoninlineReturnsVCString_Null(): VCString? = null
    suspend fun suspendNoninlineReturnsVCStringNullable_Null(): VCStringNullable? = null
    suspend fun suspendNoninlineReturnsVCAny_Null(): VCAny? = null
    suspend fun suspendNoninlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
    suspend fun suspendNoninlineReturnsVCInt_Null(): VCInt? = null
    suspend fun suspendNoninlineReturnsVCIntNullable_Null(): VCIntNullable? = null
    suspend fun suspendNoninlineAcceptsVCString(i: Int, vc: VCString) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCAny(i: Int, vc: VCAny) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCInt(i: Int, vc: VCInt) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCString_Null(i: Int, vc: VCString?) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
        result = vc
    }

    suspend fun suspendNoninlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
        result = vc
    }

    suspend inline fun suspendInlineReturnsVCString(): VCString = VCString("OK")
    suspend inline fun suspendInlineReturnsVCStringNullable(): VCStringNullable = VCStringNullable("OK")
    suspend inline fun suspendInlineReturnsVCAny(): VCAny = VCAny("OK")
    suspend inline fun suspendInlineReturnsVCAnyNullable(): VCAnyNullable = VCAnyNullable("OK")
    suspend inline fun suspendInlineReturnsVCInt(): VCInt = VCInt(42)
    suspend inline fun suspendInlineReturnsVCIntNullable(): VCIntNullable = VCIntNullable(42)
    suspend inline fun suspendInlineReturnsVCString_Null(): VCString? = null
    suspend inline fun suspendInlineReturnsVCStringNullable_Null(): VCStringNullable? = null
    suspend inline fun suspendInlineReturnsVCAny_Null(): VCAny? = null
    suspend inline fun suspendInlineReturnsVCAnyNullable_Null(): VCAnyNullable? = null
    suspend inline fun suspendInlineReturnsVCInt_Null(): VCInt? = null
    suspend inline fun suspendInlineReturnsVCIntNullable_Null(): VCIntNullable? = null
    suspend inline fun suspendInlineAcceptsVCString(i: Int, vc: VCString) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCStringNullable(i: Int, vc: VCStringNullable) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCAny(i: Int, vc: VCAny) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCAnyNullable(i: Int, vc: VCAnyNullable) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCInt(i: Int, vc: VCInt) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCIntNullable(i: Int, vc: VCIntNullable) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCString_Null(i: Int, vc: VCString?) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCStringNullable_Null(i: Int, vc: VCStringNullable?) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCAny_Null(i: Int, vc: VCAny?) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCAnyNullable_Null(i: Int, vc: VCAnyNullable?) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCInt_Null(i: Int, vc: VCInt?) {
        result = vc
    }

    suspend inline fun suspendInlineAcceptsVCIntNullable_Null(i: Int, vc: VCIntNullable?) {
        result = vc
    }
}

suspend fun test() {
    if (ordinaryNoninlineReturnsVCString() != VCString("OK")) throw IllegalStateException("ordinaryNoninlineReturnsVCString")

    if (ordinaryNoninlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("ordinaryNoninlineReturnsVCStringNullable")

    if (ordinaryNoninlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("ordinaryNoninlineReturnsVCAny")

    if (ordinaryNoninlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("ordinaryNoninlineReturnsVCAnyNullable")

    if (ordinaryNoninlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("ordinaryNoninlineReturnsVCInt")

    if (ordinaryNoninlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("ordinaryNoninlineReturnsVCIntNullable")

    if (ordinaryNoninlineReturnsVCString_Null() != null) throw IllegalStateException("ordinaryNoninlineReturnsVCString_Null")

    if (ordinaryNoninlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("ordinaryNoninlineReturnsVCStringNullable_Null")

    if (ordinaryNoninlineReturnsVCAny_Null() != null) throw IllegalStateException("ordinaryNoninlineReturnsVCAny_Null")

    if (ordinaryNoninlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("ordinaryNoninlineReturnsVCAnyNullable_Null")

    if (ordinaryNoninlineReturnsVCInt_Null() != null) throw IllegalStateException("ordinaryNoninlineReturnsVCInt_Null")

    if (ordinaryNoninlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("ordinaryNoninlineReturnsVCIntNullable_Null")

    ordinaryNoninlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("ordinaryNoninlineAcceptsVCString")

    ordinaryNoninlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("ordinaryNoninlineAcceptsVCStringNullable")

    ordinaryNoninlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("ordinaryNoninlineAcceptsVCAny")

    ordinaryNoninlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("ordinaryNoninlineAcceptsVCAnyNullable")

    ordinaryNoninlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("ordinaryNoninlineAcceptsVCInt")

    ordinaryNoninlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("ordinaryNoninlineAcceptsVCIntNullable")

    ordinaryNoninlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryNoninlineAcceptsVCString_Null")

    ordinaryNoninlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryNoninlineAcceptsVCStringNullable_Null")

    ordinaryNoninlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryNoninlineAcceptsVCAny_Null")

    ordinaryNoninlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryNoninlineAcceptsVCAnyNullable_Null")

    ordinaryNoninlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryNoninlineAcceptsVCInt_Null")

    ordinaryNoninlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryNoninlineAcceptsVCIntNullable_Null")

    if (ordinaryInlineReturnsVCString() != VCString("OK")) throw IllegalStateException("ordinaryInlineReturnsVCString")

    if (ordinaryInlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("ordinaryInlineReturnsVCStringNullable")

    if (ordinaryInlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("ordinaryInlineReturnsVCAny")

    if (ordinaryInlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("ordinaryInlineReturnsVCAnyNullable")

    if (ordinaryInlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("ordinaryInlineReturnsVCInt")

    if (ordinaryInlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("ordinaryInlineReturnsVCIntNullable")

    if (ordinaryInlineReturnsVCString_Null() != null) throw IllegalStateException("ordinaryInlineReturnsVCString_Null")

    if (ordinaryInlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("ordinaryInlineReturnsVCStringNullable_Null")

    if (ordinaryInlineReturnsVCAny_Null() != null) throw IllegalStateException("ordinaryInlineReturnsVCAny_Null")

    if (ordinaryInlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("ordinaryInlineReturnsVCAnyNullable_Null")

    if (ordinaryInlineReturnsVCInt_Null() != null) throw IllegalStateException("ordinaryInlineReturnsVCInt_Null")

    if (ordinaryInlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("ordinaryInlineReturnsVCIntNullable_Null")

    ordinaryInlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("ordinaryInlineAcceptsVCString")

    ordinaryInlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("ordinaryInlineAcceptsVCStringNullable")

    ordinaryInlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("ordinaryInlineAcceptsVCAny")

    ordinaryInlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("ordinaryInlineAcceptsVCAnyNullable")

    ordinaryInlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("ordinaryInlineAcceptsVCInt")

    ordinaryInlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("ordinaryInlineAcceptsVCIntNullable")

    ordinaryInlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryInlineAcceptsVCString_Null")

    ordinaryInlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryInlineAcceptsVCStringNullable_Null")

    ordinaryInlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryInlineAcceptsVCAny_Null")

    ordinaryInlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryInlineAcceptsVCAnyNullable_Null")

    ordinaryInlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryInlineAcceptsVCInt_Null")

    ordinaryInlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("ordinaryInlineAcceptsVCIntNullable_Null")

    if (suspendNoninlineReturnsVCString() != VCString("OK")) throw IllegalStateException("suspendNoninlineReturnsVCString")

    if (suspendNoninlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("suspendNoninlineReturnsVCStringNullable")

    if (suspendNoninlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("suspendNoninlineReturnsVCAny")

    if (suspendNoninlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("suspendNoninlineReturnsVCAnyNullable")

    if (suspendNoninlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("suspendNoninlineReturnsVCInt")

    if (suspendNoninlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("suspendNoninlineReturnsVCIntNullable")

    if (suspendNoninlineReturnsVCString_Null() != null) throw IllegalStateException("suspendNoninlineReturnsVCString_Null")

    if (suspendNoninlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("suspendNoninlineReturnsVCStringNullable_Null")

    if (suspendNoninlineReturnsVCAny_Null() != null) throw IllegalStateException("suspendNoninlineReturnsVCAny_Null")

    if (suspendNoninlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("suspendNoninlineReturnsVCAnyNullable_Null")

    if (suspendNoninlineReturnsVCInt_Null() != null) throw IllegalStateException("suspendNoninlineReturnsVCInt_Null")

    if (suspendNoninlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("suspendNoninlineReturnsVCIntNullable_Null")

    suspendNoninlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("suspendNoninlineAcceptsVCString")

    suspendNoninlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("suspendNoninlineAcceptsVCStringNullable")

    suspendNoninlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("suspendNoninlineAcceptsVCAny")

    suspendNoninlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("suspendNoninlineAcceptsVCAnyNullable")

    suspendNoninlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("suspendNoninlineAcceptsVCInt")

    suspendNoninlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("suspendNoninlineAcceptsVCIntNullable")

    suspendNoninlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("suspendNoninlineAcceptsVCString_Null")

    suspendNoninlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("suspendNoninlineAcceptsVCStringNullable_Null")

    suspendNoninlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("suspendNoninlineAcceptsVCAny_Null")

    suspendNoninlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("suspendNoninlineAcceptsVCAnyNullable_Null")

    suspendNoninlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("suspendNoninlineAcceptsVCInt_Null")

    suspendNoninlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("suspendNoninlineAcceptsVCIntNullable_Null")

    if (suspendInlineReturnsVCString() != VCString("OK")) throw IllegalStateException("suspendInlineReturnsVCString")

    if (suspendInlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("suspendInlineReturnsVCStringNullable")

    if (suspendInlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("suspendInlineReturnsVCAny")

    if (suspendInlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("suspendInlineReturnsVCAnyNullable")

    if (suspendInlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("suspendInlineReturnsVCInt")

    if (suspendInlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("suspendInlineReturnsVCIntNullable")

    if (suspendInlineReturnsVCString_Null() != null) throw IllegalStateException("suspendInlineReturnsVCString_Null")

    if (suspendInlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("suspendInlineReturnsVCStringNullable_Null")

    if (suspendInlineReturnsVCAny_Null() != null) throw IllegalStateException("suspendInlineReturnsVCAny_Null")

    if (suspendInlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("suspendInlineReturnsVCAnyNullable_Null")

    if (suspendInlineReturnsVCInt_Null() != null) throw IllegalStateException("suspendInlineReturnsVCInt_Null")

    if (suspendInlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("suspendInlineReturnsVCIntNullable_Null")

    suspendInlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("suspendInlineAcceptsVCString")

    suspendInlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("suspendInlineAcceptsVCStringNullable")

    suspendInlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("suspendInlineAcceptsVCAny")

    suspendInlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("suspendInlineAcceptsVCAnyNullable")

    suspendInlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("suspendInlineAcceptsVCInt")

    suspendInlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("suspendInlineAcceptsVCIntNullable")

    suspendInlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("suspendInlineAcceptsVCString_Null")

    suspendInlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("suspendInlineAcceptsVCStringNullable_Null")

    suspendInlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("suspendInlineAcceptsVCAny_Null")

    suspendInlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("suspendInlineAcceptsVCAnyNullable_Null")

    suspendInlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("suspendInlineAcceptsVCInt_Null")

    suspendInlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("suspendInlineAcceptsVCIntNullable_Null")

    if (C().ordinaryNoninlineReturnsVCString() != VCString("OK")) throw IllegalStateException("C().ordinaryNoninlineReturnsVCString")

    if (C().ordinaryNoninlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("C().ordinaryNoninlineReturnsVCStringNullable")

    if (C().ordinaryNoninlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("C().ordinaryNoninlineReturnsVCAny")

    if (C().ordinaryNoninlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("C().ordinaryNoninlineReturnsVCAnyNullable")

    if (C().ordinaryNoninlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("C().ordinaryNoninlineReturnsVCInt")

    if (C().ordinaryNoninlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("C().ordinaryNoninlineReturnsVCIntNullable")

    if (C().ordinaryNoninlineReturnsVCString_Null() != null) throw IllegalStateException("C().ordinaryNoninlineReturnsVCString_Null")

    if (C().ordinaryNoninlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("C().ordinaryNoninlineReturnsVCStringNullable_Null")

    if (C().ordinaryNoninlineReturnsVCAny_Null() != null) throw IllegalStateException("C().ordinaryNoninlineReturnsVCAny_Null")

    if (C().ordinaryNoninlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("C().ordinaryNoninlineReturnsVCAnyNullable_Null")

    if (C().ordinaryNoninlineReturnsVCInt_Null() != null) throw IllegalStateException("C().ordinaryNoninlineReturnsVCInt_Null")

    if (C().ordinaryNoninlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("C().ordinaryNoninlineReturnsVCIntNullable_Null")

    C().ordinaryNoninlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCString")

    C().ordinaryNoninlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCStringNullable")

    C().ordinaryNoninlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCAny")

    C().ordinaryNoninlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCAnyNullable")

    C().ordinaryNoninlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCInt")

    C().ordinaryNoninlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCIntNullable")

    C().ordinaryNoninlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCString_Null")

    C().ordinaryNoninlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCStringNullable_Null")

    C().ordinaryNoninlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCAny_Null")

    C().ordinaryNoninlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCAnyNullable_Null")

    C().ordinaryNoninlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCInt_Null")

    C().ordinaryNoninlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryNoninlineAcceptsVCIntNullable_Null")

    if (C().ordinaryInlineReturnsVCString() != VCString("OK")) throw IllegalStateException("C().ordinaryInlineReturnsVCString")

    if (C().ordinaryInlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("C().ordinaryInlineReturnsVCStringNullable")

    if (C().ordinaryInlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("C().ordinaryInlineReturnsVCAny")

    if (C().ordinaryInlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("C().ordinaryInlineReturnsVCAnyNullable")

    if (C().ordinaryInlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("C().ordinaryInlineReturnsVCInt")

    if (C().ordinaryInlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("C().ordinaryInlineReturnsVCIntNullable")

    if (C().ordinaryInlineReturnsVCString_Null() != null) throw IllegalStateException("C().ordinaryInlineReturnsVCString_Null")

    if (C().ordinaryInlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("C().ordinaryInlineReturnsVCStringNullable_Null")

    if (C().ordinaryInlineReturnsVCAny_Null() != null) throw IllegalStateException("C().ordinaryInlineReturnsVCAny_Null")

    if (C().ordinaryInlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("C().ordinaryInlineReturnsVCAnyNullable_Null")

    if (C().ordinaryInlineReturnsVCInt_Null() != null) throw IllegalStateException("C().ordinaryInlineReturnsVCInt_Null")

    if (C().ordinaryInlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("C().ordinaryInlineReturnsVCIntNullable_Null")

    C().ordinaryInlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("C().ordinaryInlineAcceptsVCString")

    C().ordinaryInlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("C().ordinaryInlineAcceptsVCStringNullable")

    C().ordinaryInlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("C().ordinaryInlineAcceptsVCAny")

    C().ordinaryInlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("C().ordinaryInlineAcceptsVCAnyNullable")

    C().ordinaryInlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("C().ordinaryInlineAcceptsVCInt")

    C().ordinaryInlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("C().ordinaryInlineAcceptsVCIntNullable")

    C().ordinaryInlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryInlineAcceptsVCString_Null")

    C().ordinaryInlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryInlineAcceptsVCStringNullable_Null")

    C().ordinaryInlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryInlineAcceptsVCAny_Null")

    C().ordinaryInlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryInlineAcceptsVCAnyNullable_Null")

    C().ordinaryInlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryInlineAcceptsVCInt_Null")

    C().ordinaryInlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().ordinaryInlineAcceptsVCIntNullable_Null")

    if (C().suspendNoninlineReturnsVCString() != VCString("OK")) throw IllegalStateException("C().suspendNoninlineReturnsVCString")

    if (C().suspendNoninlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("C().suspendNoninlineReturnsVCStringNullable")

    if (C().suspendNoninlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("C().suspendNoninlineReturnsVCAny")

    if (C().suspendNoninlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("C().suspendNoninlineReturnsVCAnyNullable")

    if (C().suspendNoninlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("C().suspendNoninlineReturnsVCInt")

    if (C().suspendNoninlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("C().suspendNoninlineReturnsVCIntNullable")

    if (C().suspendNoninlineReturnsVCString_Null() != null) throw IllegalStateException("C().suspendNoninlineReturnsVCString_Null")

    if (C().suspendNoninlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("C().suspendNoninlineReturnsVCStringNullable_Null")

    if (C().suspendNoninlineReturnsVCAny_Null() != null) throw IllegalStateException("C().suspendNoninlineReturnsVCAny_Null")

    if (C().suspendNoninlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("C().suspendNoninlineReturnsVCAnyNullable_Null")

    if (C().suspendNoninlineReturnsVCInt_Null() != null) throw IllegalStateException("C().suspendNoninlineReturnsVCInt_Null")

    if (C().suspendNoninlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("C().suspendNoninlineReturnsVCIntNullable_Null")

    C().suspendNoninlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("C().suspendNoninlineAcceptsVCString")

    C().suspendNoninlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("C().suspendNoninlineAcceptsVCStringNullable")

    C().suspendNoninlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("C().suspendNoninlineAcceptsVCAny")

    C().suspendNoninlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("C().suspendNoninlineAcceptsVCAnyNullable")

    C().suspendNoninlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("C().suspendNoninlineAcceptsVCInt")

    C().suspendNoninlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("C().suspendNoninlineAcceptsVCIntNullable")

    C().suspendNoninlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendNoninlineAcceptsVCString_Null")

    C().suspendNoninlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendNoninlineAcceptsVCStringNullable_Null")

    C().suspendNoninlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendNoninlineAcceptsVCAny_Null")

    C().suspendNoninlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendNoninlineAcceptsVCAnyNullable_Null")

    C().suspendNoninlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendNoninlineAcceptsVCInt_Null")

    C().suspendNoninlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendNoninlineAcceptsVCIntNullable_Null")

    if (C().suspendInlineReturnsVCString() != VCString("OK")) throw IllegalStateException("C().suspendInlineReturnsVCString")

    if (C().suspendInlineReturnsVCStringNullable() != VCStringNullable("OK")) throw IllegalStateException("C().suspendInlineReturnsVCStringNullable")

    if (C().suspendInlineReturnsVCAny() != VCAny("OK")) throw IllegalStateException("C().suspendInlineReturnsVCAny")

    if (C().suspendInlineReturnsVCAnyNullable() != VCAnyNullable("OK")) throw IllegalStateException("C().suspendInlineReturnsVCAnyNullable")

    if (C().suspendInlineReturnsVCInt() != VCInt(42)) throw IllegalStateException("C().suspendInlineReturnsVCInt")

    if (C().suspendInlineReturnsVCIntNullable() != VCIntNullable(42)) throw IllegalStateException("C().suspendInlineReturnsVCIntNullable")

    if (C().suspendInlineReturnsVCString_Null() != null) throw IllegalStateException("C().suspendInlineReturnsVCString_Null")

    if (C().suspendInlineReturnsVCStringNullable_Null() != null) throw IllegalStateException("C().suspendInlineReturnsVCStringNullable_Null")

    if (C().suspendInlineReturnsVCAny_Null() != null) throw IllegalStateException("C().suspendInlineReturnsVCAny_Null")

    if (C().suspendInlineReturnsVCAnyNullable_Null() != null) throw IllegalStateException("C().suspendInlineReturnsVCAnyNullable_Null")

    if (C().suspendInlineReturnsVCInt_Null() != null) throw IllegalStateException("C().suspendInlineReturnsVCInt_Null")

    if (C().suspendInlineReturnsVCIntNullable_Null() != null) throw IllegalStateException("C().suspendInlineReturnsVCIntNullable_Null")

    C().suspendInlineAcceptsVCString(1, VCString("OK"))
    if (result != VCString("OK")) throw IllegalStateException("C().suspendInlineAcceptsVCString")

    C().suspendInlineAcceptsVCStringNullable(1, VCStringNullable("OK"))
    if (result != VCStringNullable("OK")) throw IllegalStateException("C().suspendInlineAcceptsVCStringNullable")

    C().suspendInlineAcceptsVCAny(1, VCAny("OK"))
    if (result != VCAny("OK")) throw IllegalStateException("C().suspendInlineAcceptsVCAny")

    C().suspendInlineAcceptsVCAnyNullable(1, VCAnyNullable("OK"))
    if (result != VCAnyNullable("OK")) throw IllegalStateException("C().suspendInlineAcceptsVCAnyNullable")

    C().suspendInlineAcceptsVCInt(1, VCInt(42))
    if (result != VCInt(42)) throw IllegalStateException("C().suspendInlineAcceptsVCInt")

    C().suspendInlineAcceptsVCIntNullable(1, VCIntNullable(42))
    if (result != VCIntNullable(42)) throw IllegalStateException("C().suspendInlineAcceptsVCIntNullable")

    C().suspendInlineAcceptsVCString_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendInlineAcceptsVCString_Null")

    C().suspendInlineAcceptsVCStringNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendInlineAcceptsVCStringNullable_Null")

    C().suspendInlineAcceptsVCAny_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendInlineAcceptsVCAny_Null")

    C().suspendInlineAcceptsVCAnyNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendInlineAcceptsVCAnyNullable_Null")

    C().suspendInlineAcceptsVCInt_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendInlineAcceptsVCInt_Null")

    C().suspendInlineAcceptsVCIntNullable_Null(1, null)
    if (result != null) throw IllegalStateException("C().suspendInlineAcceptsVCIntNullable_Null")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        test()
    }
    return "OK"
}
