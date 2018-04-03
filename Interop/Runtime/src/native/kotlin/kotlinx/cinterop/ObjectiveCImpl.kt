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

@ExportTypeInfo("theForeignObjCObjectTypeInfo")
internal open class ForeignObjCObject

abstract class ObjCObjectBase protected constructor() : ObjCObject
abstract class ObjCObjectBaseMeta protected constructor() : ObjCObjectBase(), ObjCObjectMeta {}

fun optional(): Nothing = throw RuntimeException("Do not call me!!!")

@konan.internal.Intrinsic
external fun <T : ObjCObjectBase> T.initBy(constructorCall: T): T

@konan.internal.ExportForCompiler
private fun ObjCObjectBase.superInitCheck(superInitCallResult: ObjCObject?) {
    if (superInitCallResult == null)
        throw RuntimeException("Super initialization failed")

    if (superInitCallResult.rawPtr() != this.rawPtr())
        throw UnsupportedOperationException("Super initializer has replaced object")
}

@Deprecated("Use plain Kotlin cast", ReplaceWith("this as T"), DeprecationLevel.WARNING)
fun <T : Any?> Any?.uncheckedCast(): T = @Suppress("UNCHECKED_CAST") (this as T) // TODO: make private

@SymbolName("Kotlin_Interop_refFromObjC")
external fun <T : ObjCObject?> interpretObjCPointerOrNull(rawPtr: NativePtr): T?

inline fun <T : ObjCObject> interpretObjCPointer(rawPtr: NativePtr): T = interpretObjCPointerOrNull<T>(rawPtr)!!

@SymbolName("Kotlin_Interop_refToObjC")
external fun ObjCObject?.rawPtr(): NativePtr

@SymbolName("Kotlin_Interop_createKotlinObjectHolder")
external fun createKotlinObjectHolder(any: Any?): NativePtr

inline fun <reified T : Any> unwrapKotlinObjectHolder(holder: ObjCObject?): T {
    return unwrapKotlinObjectHolderImpl(holder!!.rawPtr()) as T
}

@PublishedApi
@SymbolName("Kotlin_Interop_unwrapKotlinObjectHolder")
external internal fun unwrapKotlinObjectHolderImpl(ptr: NativePtr): Any

class ObjCObjectVar<T : ObjCObject?>(rawPtr: NativePtr) : CVariable(rawPtr) {
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

class ObjCNotImplementedVar<T : Any?>(rawPtr: NativePtr) : CVariable(rawPtr) {
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

var <T : Any?> ObjCNotImplementedVar<T>.value: T
    get() = TODO()
    set(value) = TODO()

typealias ObjCStringVarOf<T> = ObjCNotImplementedVar<T>
typealias ObjCBlockVar<T> = ObjCNotImplementedVar<T>

@konan.internal.Intrinsic external fun getReceiverOrSuper(receiver: NativePtr, superClass: NativePtr): COpaquePointer?

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExternalObjCClass(val protocolGetter: String = "")

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

@konan.internal.ExportForCppRuntime("Kotlin_Interop_getObjCClass")
private fun getObjCClassByName(name: NativePtr): NativePtr {
    val result = objc_lookUpClass(name)
    if (result == nativeNullPtr) {
        val className = interpretCPointer<ByteVar>(name)!!.toKString()
        val message = """Objective-C class '$className' not found.
            |Ensure that the containing framework or library was linked.""".trimMargin()

        throw RuntimeException(message)
    }
    return result
}

@konan.internal.ExportForCompiler
private fun allocObjCObject(clazz: NativePtr): NativePtr {
    val rawResult = objc_allocWithZone(clazz)
    if (rawResult == nativeNullPtr) {
        throw OutOfMemoryError("Unable to allocate Objective-C object")
    }

    // Note: `objc_allocWithZone` returns retained pointer, and thus it must be balanced by the caller.

    return rawResult
}

@konan.internal.Intrinsic
@konan.internal.ExportForCompiler
private external fun <T : ObjCObject> getObjCClass(): NativePtr

@konan.internal.Intrinsic external fun getMessenger(superClass: NativePtr): COpaquePointer?
@konan.internal.Intrinsic external fun getMessengerLU(superClass: NativePtr): COpaquePointer?

// Konan runtme:

@SymbolName("Kotlin_Interop_CreateNSStringFromKString")
external fun CreateNSStringFromKString(str: String?): NativePtr

@SymbolName("Kotlin_Interop_CreateKStringFromNSString")
external fun CreateKStringFromNSString(ptr: NativePtr): String?

@SymbolName("Kotlin_Interop_ObjCToString")
private external fun ObjCToString(ptr: NativePtr): String

@SymbolName("Kotlin_Interop_ObjCHashCode")
private external fun ObjCHashCode(ptr: NativePtr): Int

@SymbolName("Kotlin_Interop_ObjCEquals")
private external fun ObjCEquals(ptr: NativePtr, otherPtr: NativePtr): Boolean


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

@SymbolName("Kotlin_objc_lookUpClass")
external fun objc_lookUpClass(name: NativePtr): NativePtr
