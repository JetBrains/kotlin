/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.cinterop

interface ObjCObject
interface ObjCClass : ObjCObject
typealias ObjCObjectMeta = ObjCClass

abstract class ObjCObjectBase protected constructor() : ObjCObject {
    final override fun equals(other: Any?): Boolean = TODO()
    final override fun hashCode(): Int = TODO()
    final override fun toString(): String = TODO()
}

abstract class ObjCObjectBaseMeta protected constructor() : ObjCObjectBase(), ObjCObjectMeta {}

fun optional(): Nothing = throw RuntimeException("Do not call me!!!")

/**
 * The runtime representation of any [ObjCObject].
 */
@ExportTypeInfo("theObjCPointerHolderTypeInfo")
class ObjCPointerHolder(inline val rawPtr: NativePtr) {
    init {
        assert(rawPtr != nativeNullPtr)
        objc_retain(rawPtr)
    }

    final override fun equals(other: Any?): Boolean = TODO()
    final override fun hashCode(): Int = TODO()
    final override fun toString(): String = TODO()
}

@konan.internal.Intrinsic
@konan.internal.ExportForCompiler
private external fun ObjCObject.initFromPtr(ptr: NativePtr)

@konan.internal.ExportForCompiler
private fun ObjCObject.initFrom(other: ObjCObject?) = this.initFromPtr(other!!.rawPtr)

fun <T : Any?> Any?.uncheckedCast(): T = @Suppress("UNCHECKED_CAST") (this as T) // TODO: make private

inline fun <T : ObjCObject?> interpretObjCPointerOrNull(rawPtr: NativePtr): T? = if (rawPtr != nativeNullPtr) {
    ObjCPointerHolder(rawPtr).uncheckedCast<T>()
} else {
    null
}

inline fun <T : ObjCObject> interpretObjCPointer(rawPtr: NativePtr): T = if (rawPtr != nativeNullPtr) {
    ObjCPointerHolder(rawPtr).uncheckedCast<T>()
} else {
    throw NullPointerException()
}

inline val ObjCObject.rawPtr: NativePtr get() = (this.uncheckedCast<ObjCPointerHolder>()).rawPtr
inline val ObjCObject?.rawPtr: NativePtr get() = if (this != null) {
    (this.uncheckedCast<ObjCPointerHolder>()).rawPtr
} else {
    nativeNullPtr
}

class ObjCObjectVar<T : ObjCObject?>(rawPtr: NativePtr) : CVariable(rawPtr) {
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

class ObjCStringVarOf<T : String?>(rawPtr: NativePtr) : CVariable(rawPtr) {
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

var <T : String?> ObjCStringVarOf<T>.value: T
    get() = TODO()
    set(value) = TODO()

@konan.internal.Intrinsic external fun getReceiverOrSuper(receiver: NativePtr, superClass: NativePtr): COpaquePointer?

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExternalObjCClass()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ObjCMethod(val selector: String, val bridge: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ObjCBridge(val selector: String, val encoding: String, val imp: String)

@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
annotation class ObjCConstructor(val initSelector: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class InteropStubs()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
private annotation class ObjCMethodImp(val selector: String, val encoding: String)

@konan.internal.ExportForCompiler
private fun <T : ObjCObject> allocObjCObject(clazz: NativePtr): T {
    val rawResult = objc_allocWithZone(clazz)
    if (rawResult == nativeNullPtr) {
        throw OutOfMemoryError("Unable to allocate Objective-C object")
    }

    val result = interpretObjCPointerOrNull<T>(rawResult)!!
    // `objc_allocWithZone` returns retained pointer. Balance it:
    objc_release(rawResult)
    // TODO: do not retain this pointer in `interpretObjCPointerOrNull` instead.

    return result
}

@konan.internal.Intrinsic
@konan.internal.ExportForCompiler
private external fun <T : ObjCObject> getObjCClass(): NativePtr

@konan.internal.Intrinsic external fun getMessenger(superClass: NativePtr): COpaquePointer?
@konan.internal.Intrinsic external fun getMessengerLU(superClass: NativePtr): COpaquePointer?

// Konan runtme:

@SymbolName("CreateNSStringFromKString")
external fun CreateNSStringFromKString(str: String?): NativePtr

@SymbolName("CreateKStringFromNSString")
external fun CreateKStringFromNSString(ptr: NativePtr): String?

// Objective-C runtime:

@SymbolName("objc_retainAutoreleaseReturnValue")
external fun objc_retainAutoreleaseReturnValue(ptr: NativePtr): NativePtr

@SymbolName("Kotlin_objc_autoreleasePoolPush")
external fun objc_autoreleasePoolPush(): NativePtr

@SymbolName("Kotlin_objc_autoreleasePoolPop")
external fun objc_autoreleasePoolPop(ptr: NativePtr)

@SymbolName("Kotlin_objc_allocWithZone")
private external fun objc_allocWithZone(clazz: NativePtr): NativePtr

@SymbolName("Kotlin_objc_retain")
external fun objc_retain(ptr: NativePtr): NativePtr

@SymbolName("Kotlin_objc_release")
external fun objc_release(ptr: NativePtr)
