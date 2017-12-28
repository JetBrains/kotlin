package kotlinx.wasm.jsinterop

import konan.internal.ExportForCppRuntime
import kotlinx.cinterop.*

typealias Arena = Int
typealias Object = Int
typealias Pointer = Int

@SymbolName("Konan_js_allocateArena")
external public fun allocateArena(): Arena

@SymbolName("Konan_js_freeArena")
external public fun freeArena(arena: Arena)

@SymbolName("Konan_js_pushIntToArena")
external public fun pushIntToArena(arena: Arena, value: Int)

const val upperWord = 0xffffffff.toLong() shl 32

fun doubleUpper(value: Double): Int =
    ((value.toBits() and upperWord) ushr 32) .toInt()

fun doubleLower(value: Double): Int =
    (value.toBits() and 0x00000000ffffffff) .toInt()

@SymbolName("ReturnSlot_getDouble")
external public fun ReturnSlot_getDouble(): Double

@SymbolName("Kotlin_String_utf16pointer")
external public fun stringPointer(message: String): Pointer

@SymbolName("Kotlin_String_utf16length")
external public fun stringLengthBytes(message: String): Int

typealias KtFunction <R> = ((ArrayList<JsValue>)->R)

fun <R> wrapFunction(func: KtFunction<R>): Int {
    val ptr: Long = StableRef.create(func).asCPointer().toLong() 
    return ptr.toInt() // TODO: LP64 unsafe.
}

@ExportForCppRuntime("Konan_js_runLambda")
fun runLambda(pointer: Int, argumentsArena: Arena, argumentsArenaSize: Int): Int {
    val arguments = arrayListOf<JsValue>()
    for (i in 0 until argumentsArenaSize) {
        arguments.add(JsValue(argumentsArena, i));
    }
    val previousArena = ArenaManager.currentArena
    ArenaManager.currentArena = argumentsArena
    // TODO: LP64 unsafe: wasm32 passes Int, not Long.
    val func = StableRef.fromValue(pointer.toLong().toCPointer()!!).get() as KtFunction<JsValue>
    val result = func(arguments)

    ArenaManager.currentArena = previousArena
    return result.index
}

open class JsValue(val arena: Arena, val index: Object) {
    fun getInt(property: String): Int {
        return getInt(ArenaManager.currentArena, index, stringPointer(property), stringLengthBytes(property))
    }
    fun getProperty(property: String): JsValue {
        return JsValue(ArenaManager.currentArena, Konan_js_getProperty(ArenaManager.currentArena, index, stringPointer(property), stringLengthBytes(property)))
    }
}

open class JsArray(arena: Arena, index: Object): JsValue(arena, index) {
    constructor(jsValue: JsValue): this(jsValue.arena, jsValue.index)        
    operator fun get(index: Int): JsValue {
        // TODO: we could pass an integer index to index arrays.
        return getProperty(index.toString())
    }
    val size: Int
        get() = this.getInt("length")
}

@SymbolName("Konan_js_getInt")
external public fun getInt(arena: Arena, obj: Object, propertyPtr: Pointer, propertyLen: Int): Int;

@SymbolName("Konan_js_getProperty")
external public fun Konan_js_getProperty(arena: Arena, obj: Object, propertyPtr: Pointer, propertyLen: Int): Int;

@SymbolName("Konan_js_setFunction")
external public fun setFunction(arena: Arena, obj: Object, propertyName: Pointer, propertyLength: Int , function: Int)

@SymbolName("Konan_js_setString")
external public fun setString(arena: Arena, obj: Object, propertyName: Pointer, propertyLength: Int, stringPtr: Pointer, stringLength: Int )

fun setter(obj: JsValue, property: String, string: String) {
    setString(obj.arena, obj.index, stringPointer(property), stringLengthBytes(property), stringPointer(string), stringLengthBytes(string))
}

fun setter(obj: JsValue, property: String, lambda: KtFunction<Unit>) {
    val pointer = wrapFunction(lambda);
    setFunction(obj.arena, obj.index, stringPointer(property), stringLengthBytes(property), pointer)
}

fun JsValue.setter(property: String, lambda: KtFunction<Unit>) {
    setter(this, property, lambda)
}

fun JsValue.setter(property: String, string: String) {
    setter(this, property, string)
}

object ArenaManager {
    val globalArena = allocateArena()
    var currentArena = globalArena
}
