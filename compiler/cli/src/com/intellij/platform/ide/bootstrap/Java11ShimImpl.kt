/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.platform.ide.bootstrap

import com.google.common.annotations.VisibleForTesting
import com.intellij.util.Java11Shim
import com.intellij.util.containers.ConcurrentLongObjectMap

@ApiStatus.Internal
@VisibleForTesting
@Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
class Java11ShimImpl : Java11Shim {
  private val walker = StackWalker.getInstance(setOf(StackWalker.Option.RETAIN_CLASS_REFERENCE), 5)

  override fun <K : Any, V> copyOf(map: Map<K, V>): Map<K, V> = java.util.Map.copyOf(map)

  override fun <K : Any, V> mapOf(k: K, v: V): Map<K, V> = java.util.Map.of(k, v)

  override fun <E> copyOf(collection: Collection<E>): Set<E> = java.util.Set.copyOf(collection)

  override fun <E> copyOfList(collection: Collection<E>): List<E> = java.util.List.copyOf(collection)

  override fun <K : Any, V> mapOf(): Map<K, V> = java.util.Map.of()

  override fun <K : Any, V> mapOf(k: K, v: V, k2: K, v2: V): Map<K, V> = java.util.Map.of(k, v, k2, v2)

  override fun <V : Any> createConcurrentLongObjectMap(): ConcurrentLongObjectMap<V> {
    return ConcurrentCollectionFactory.createConcurrentLongObjectMap()
  }

  override fun <E> listOf(): List<E> = java.util.List.of()

  override fun <E> listOf(element: E): List<E> = java.util.List.of(element)

  override fun <E> listOf(e1: E, e2: E): List<E> = java.util.List.of(e1, e2)

  override fun <E> listOf(array: Array<E>, size: Int): List<E> {
    return when (size) {
      1 -> java.util.List.of(array[0])
      2 -> java.util.List.of(array[0], array[1])
      3 -> java.util.List.of(array[0], array[1], array[2])
      4 -> java.util.List.of(array[0], array[1], array[2], array[3])
      5 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4])
      6 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4], array[5])
      7 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4], array[5], array[6])
      8 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7])
      9 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8])
      10 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8], array[9])
      11 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8], array[9], array[10])
      12 -> java.util.List.of(array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8], array[9], array[10], array[11])
      else -> {
        if (array.size == size) {
          java.util.List.of(*array)
        }
        else {
          val newResult = arrayOfNulls<Any>(size)
          System.arraycopy(array, 0, newResult, 0, size)
          @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
          return java.util.List.of(*newResult) as List<E>
        }
      }
    }
  }

  override fun getCallerClass(stackFrameIndex: Int): Class<*>? {
    return walker.walk { stream ->
      stream.skip(stackFrameIndex.toLong()).map { it.declaringClass }.findFirst().orElse(null)
    }
  }
}