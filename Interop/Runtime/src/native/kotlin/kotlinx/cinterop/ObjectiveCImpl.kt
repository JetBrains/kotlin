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
import kotlin.native.*
import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.FilterExceptions

interface ObjCObject
interface ObjCClass : ObjCObject
interface ObjCClassOf<T : ObjCObject> : ObjCClass // TODO: T should be added to ObjCClass and all meta-classes instead.
typealias ObjCObjectMeta = ObjCClass

interface ObjCProtocol : ObjCObject

@ExportTypeInfo("theForeignObjCObjectTypeInfo")
@kotlin.native.internal.Frozen
internal open class ForeignObjCObject : kotlin.native.internal.ObjCObjectWrapper

abstract class ObjCObjectBase protected constructor() : ObjCObject {
    @Target(AnnotationTarget.CONSTRUCTOR)
    @Retention(AnnotationRetention.SOURCE)
    annotation class OverrideInit
}
abstract class ObjCObjectBaseMeta protected constructor() : ObjCObjectBase(), ObjCObjectMeta {}

fun optional(): Nothing = throw RuntimeException("Do not call me!!!")

@Deprecated(
        "Add @OverrideInit to constructor to make it override Objective-C initializer",
        level = DeprecationLevel.ERROR
)
@TypedIntrinsic(IntrinsicType.OBJC_INIT_BY)
external fun <T : ObjCObjectBase> T.initBy(constructorCall: T): T

@kotlin.native.internal.ExportForCompiler
private fun ObjCObjectBase.superInitCheck(superInitCallResult: ObjCObject?) {
    if (superInitCallResult == null)
        throw RuntimeException("Super initialization failed")

    if (superInitCallResult.objcPtr() != this.objcPtr())
        throw UnsupportedOperationException("Super initializer has replaced object")
}

internal fun <T : Any?> Any?.uncheckedCast(): T = @Suppress("UNCHECKED_CAST") (this as T)

// Note: if this is called for non-frozen object on a wrong worker, the program will terminate.
@SymbolName("Kotlin_Interop_refFromObjC")
external fun <T> interpretObjCPointerOrNull(objcPtr: NativePtr): T?

@ExportForCppRuntime
inline fun <T : Any> interpretObjCPointer(objcPtr: NativePtr): T = interpretObjCPointerOrNull<T>(objcPtr)!!

@SymbolName("Kotlin_Interop_refToObjC")
external fun Any?.objcPtr(): NativePtr

@SymbolName("Kotlin_Interop_createKotlinObjectHolder")
external fun createKotlinObjectHolder(any: Any?): NativePtr

// Note: if this is called for non-frozen underlying ref on a wrong worker, the program will terminate.
inline fun <reified T : Any> unwrapKotlinObjectHolder(holder: Any?): T {
    return unwrapKotlinObjectHolderImpl(holder!!.objcPtr()) as T
}

@PublishedApi
@SymbolName("Kotlin_Interop_unwrapKotlinObjectHolder")
external internal fun unwrapKotlinObjectHolderImpl(ptr: NativePtr): Any

class ObjCObjectVar<T>(rawPtr: NativePtr) : CVariable(rawPtr) {
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

@TypedIntrinsic(IntrinsicType.OBJC_CREATE_SUPER_STRUCT)
@PublishedApi
internal external fun createObjCSuperStruct(receiver: NativePtr, superClass: NativePtr): NativePtr

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExternalObjCClass(val protocolGetter: String = "", val binaryName: String = "")

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
annotation class ObjCMethod(val selector: String, val encoding: String, val isStret: Boolean = false)

@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
annotation class ObjCConstructor(val initSelector: String, val designated: Boolean)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ObjCFactory(val selector: String, val encoding: String, val isStret: Boolean = false)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class InteropStubs()

@PublishedApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
internal annotation class ObjCMethodImp(val selector: String, val encoding: String)

@PublishedApi
@TypedIntrinsic(IntrinsicType.OBJC_GET_SELECTOR)
internal external fun objCGetSelector(selector: String): COpaquePointer

@kotlin.native.internal.ExportForCppRuntime("Kotlin_Interop_getObjCClass")
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

@kotlin.native.internal.ExportForCompiler
private fun allocObjCObject(clazz: NativePtr): NativePtr {
    val rawResult = objc_allocWithZone(clazz)
    if (rawResult == nativeNullPtr) {
        throw OutOfMemoryError("Unable to allocate Objective-C object")
    }

    // Note: `objc_allocWithZone` returns retained pointer, and thus it must be balanced by the caller.

    return rawResult
}

@TypedIntrinsic(IntrinsicType.OBJC_GET_OBJC_CLASS)
@kotlin.native.internal.ExportForCompiler
private external fun <T : ObjCObject> getObjCClass(): NativePtr

@PublishedApi
@TypedIntrinsic(IntrinsicType.OBJC_GET_MESSENGER)
internal external fun getMessenger(superClass: NativePtr): COpaquePointer?

@PublishedApi
@TypedIntrinsic(IntrinsicType.OBJC_GET_MESSENGER_STRET)
internal external fun getMessengerStret(superClass: NativePtr): COpaquePointer?


internal class ObjCWeakReferenceImpl : kotlin.native.ref.WeakReferenceImpl() {
    @SymbolName("Konan_ObjCInterop_getWeakReference")
    external override fun get(): Any?
}

@SymbolName("Konan_ObjCInterop_initWeakReference")
private external fun ObjCWeakReferenceImpl.init(objcPtr: NativePtr)

@kotlin.native.internal.ExportForCppRuntime internal fun makeObjCWeakReferenceImpl(objcPtr: NativePtr): ObjCWeakReferenceImpl {
    val result = ObjCWeakReferenceImpl()
    result.init(objcPtr)
    return result
}

// Konan runtme:

@Deprecated("Use plain Kotlin cast of String to NSString", level = DeprecationLevel.ERROR)
@SymbolName("Kotlin_Interop_CreateNSStringFromKString")
external fun CreateNSStringFromKString(str: String?): NativePtr

@Deprecated("Use plain Kotlin cast of NSString to String", level = DeprecationLevel.ERROR)
@SymbolName("Kotlin_Interop_CreateKStringFromNSString")
external fun CreateKStringFromNSString(ptr: NativePtr): String?

@PublishedApi
@SymbolName("Kotlin_Interop_CreateObjCObjectHolder")
internal external fun createObjCObjectHolder(ptr: NativePtr): Any?

// Objective-C runtime:

@SymbolName("objc_retainAutoreleaseReturnValue")
external fun objc_retainAutoreleaseReturnValue(ptr: NativePtr): NativePtr

@SymbolName("Kotlin_objc_autoreleasePoolPush")
external fun objc_autoreleasePoolPush(): NativePtr

@SymbolName("Kotlin_objc_autoreleasePoolPop")
external fun objc_autoreleasePoolPop(ptr: NativePtr)

@SymbolName("Kotlin_objc_allocWithZone")
@FilterExceptions
private external fun objc_allocWithZone(clazz: NativePtr): NativePtr

@SymbolName("Kotlin_objc_retain")
external fun objc_retain(ptr: NativePtr): NativePtr

@SymbolName("Kotlin_objc_release")
external fun objc_release(ptr: NativePtr)

@SymbolName("Kotlin_objc_lookUpClass")
external fun objc_lookUpClass(name: NativePtr): NativePtr
