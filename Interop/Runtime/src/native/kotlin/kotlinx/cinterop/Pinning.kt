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

package kotlinx.cinterop

data class Pinned<out T : Any> internal constructor(private val stablePtr: COpaquePointer) {

    /**
     * Disposes the handle. It must not be [used][get] after that.
     */
    fun unpin() {
        disposeStablePointer(this.stablePtr)
    }

    /**
     * Returns the underlying pinned object.
     */
    fun get(): T = @Suppress("UNCHECKED_CAST") (derefStablePointer(stablePtr) as T)

}

fun <T : Any> T.pin() = Pinned<T>(createStablePointer(this))

inline fun <T : Any, R> T.usePinned(block: (Pinned<T>) -> R): R {
    val pinned = this.pin()
    return try {
        block(pinned)
    } finally {
        pinned.unpin()
    }
}

fun Pinned<ByteArray>.addressOf(index: Int): CPointer<ByteVar> = this.addressOfElement(index)
fun ByteArray.refTo(index: Int): CValuesRef<ByteVar> = this.usingPinned { addressOf(index) }

fun Pinned<ShortArray>.addressOf(index: Int): CPointer<ShortVar> = this.addressOfElement(index)
fun ShortArray.refTo(index: Int): CValuesRef<ShortVar> = this.usingPinned { addressOf(index) }

fun Pinned<IntArray>.addressOf(index: Int): CPointer<IntVar> = this.addressOfElement(index)
fun IntArray.refTo(index: Int): CValuesRef<IntVar> = this.usingPinned { addressOf(index) }

fun Pinned<LongArray>.addressOf(index: Int): CPointer<LongVar> = this.addressOfElement(index)
fun LongArray.refTo(index: Int): CValuesRef<LongVar> = this.usingPinned { addressOf(index) }

fun Pinned<FloatArray>.addressOf(index: Int): CPointer<FloatVar> = this.addressOfElement(index)
fun FloatArray.refTo(index: Int): CValuesRef<FloatVar> = this.usingPinned { addressOf(index) }

fun Pinned<DoubleArray>.addressOf(index: Int): CPointer<DoubleVar> = this.addressOfElement(index)
fun DoubleArray.refTo(index: Int): CValuesRef<DoubleVar> = this.usingPinned { addressOf(index) }

private inline fun <T : Any, P : CPointed> T.usingPinned(
        crossinline block: Pinned<T>.() -> CPointer<P>
) = object : CValuesRef<P>() {

    override fun getPointer(scope: AutofreeScope): CPointer<P> {
        val pinned = this@usingPinned.pin()
        scope.defer { pinned.unpin() }
        return pinned.block()
    }
}

@SymbolName("Kotlin_Arrays_getAddressOfElement")
private external fun getAddressOfElement(array: Any, index: Int): COpaquePointer

@Suppress("NOTHING_TO_INLINE")
private inline fun <P : CVariable> Pinned<*>.addressOfElement(index: Int): CPointer<P> =
        getAddressOfElement(this.get(), index).reinterpret()
