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

@file:Suppress("FINAL_UPPER_BOUND", "NOTHING_TO_INLINE")

package kotlinx.cinterop

@JvmName("plus\$Byte")
inline operator fun <T : ByteVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 1)

@JvmName("plus\$Byte")
inline operator fun <T : ByteVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$Byte")
inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@JvmName("set\$Byte")
inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$Byte")
inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@JvmName("set\$Byte")
inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$Short")
inline operator fun <T : ShortVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 2)

@JvmName("plus\$Short")
inline operator fun <T : ShortVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$Short")
inline operator fun <T : Short> CPointer<ShortVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@JvmName("set\$Short")
inline operator fun <T : Short> CPointer<ShortVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$Short")
inline operator fun <T : Short> CPointer<ShortVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@JvmName("set\$Short")
inline operator fun <T : Short> CPointer<ShortVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$Int")
inline operator fun <T : IntVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4)

@JvmName("plus\$Int")
inline operator fun <T : IntVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$Int")
inline operator fun <T : Int> CPointer<IntVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@JvmName("set\$Int")
inline operator fun <T : Int> CPointer<IntVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$Int")
inline operator fun <T : Int> CPointer<IntVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@JvmName("set\$Int")
inline operator fun <T : Int> CPointer<IntVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$Long")
inline operator fun <T : LongVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8)

@JvmName("plus\$Long")
inline operator fun <T : LongVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$Long")
inline operator fun <T : Long> CPointer<LongVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@JvmName("set\$Long")
inline operator fun <T : Long> CPointer<LongVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$Long")
inline operator fun <T : Long> CPointer<LongVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@JvmName("set\$Long")
inline operator fun <T : Long> CPointer<LongVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$UByte")
inline operator fun <T : UByteVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 1)

@JvmName("plus\$UByte")
inline operator fun <T : UByteVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$UByte")
inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$UShort")
inline operator fun <T : UShortVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 2)

@JvmName("plus\$UShort")
inline operator fun <T : UShortVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$UShort")
inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$UShort")
inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$UInt")
inline operator fun <T : UIntVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4)

@JvmName("plus\$UInt")
inline operator fun <T : UIntVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$UInt")
inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$UInt")
inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$ULong")
inline operator fun <T : ULongVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8)

@JvmName("plus\$ULong")
inline operator fun <T : ULongVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$ULong")
inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$ULong")
inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$Float")
inline operator fun <T : FloatVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4)

@JvmName("plus\$Float")
inline operator fun <T : FloatVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$Float")
inline operator fun <T : Float> CPointer<FloatVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@JvmName("set\$Float")
inline operator fun <T : Float> CPointer<FloatVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$Float")
inline operator fun <T : Float> CPointer<FloatVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@JvmName("set\$Float")
inline operator fun <T : Float> CPointer<FloatVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("plus\$Double")
inline operator fun <T : DoubleVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8)

@JvmName("plus\$Double")
inline operator fun <T : DoubleVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@JvmName("get\$Double")
inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@JvmName("set\$Double")
inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@JvmName("get\$Double")
inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@JvmName("set\$Double")
inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

/* Generated by:

#!/bin/bash

function gen {
echo "@JvmName(\"plus\\\$$1\")"
echo "inline operator fun <T : ${1}VarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? ="
echo "        interpretCPointer(this.rawValue + index * ${2})"
echo
echo "@JvmName(\"plus\\\$$1\")"
echo "inline operator fun <T : ${1}VarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? ="
echo "        this + index.toLong()"
echo
echo "@JvmName(\"get\\\$$1\")"
echo "inline operator fun <T : $1> CPointer<${1}VarOf<T>>.get(index: Int): T ="
echo "        (this + index)!!.pointed.value"
echo
echo "@JvmName(\"set\\\$$1\")"
echo "inline operator fun <T : $1> CPointer<${1}VarOf<T>>.set(index: Int, value: T) {"
echo "    (this + index)!!.pointed.value = value"
echo '}'
echo
echo "@JvmName(\"get\\\$$1\")"
echo "inline operator fun <T : $1> CPointer<${1}VarOf<T>>.get(index: Long): T ="
echo "        (this + index)!!.pointed.value"
echo
echo "@JvmName(\"set\\\$$1\")"
echo "inline operator fun <T : $1> CPointer<${1}VarOf<T>>.set(index: Long, value: T) {"
echo "    (this + index)!!.pointed.value = value"
echo '}'
echo
}

gen Byte 1
gen Short 2
gen Int 4
gen Long 8
gen UByte 1
gen UShort 2
gen UInt 4
gen ULong 8
gen Float 4
gen Double 8

 */
