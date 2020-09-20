/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.ktype.nonReified

import kotlin.test.*
import kotlin.reflect.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun <T> foo() = typeOf<List<T>>()

@Test
fun test_fun() {
    val l = foo<Int>()
    assertEquals(List::class, l.classifier)
    val t = l.arguments.single().type!!.classifier
    assertTrue(t is KTypeParameter)
    assertFalse((t as KTypeParameter).isReified)
    assertEquals("T", (t as KTypeParameter).name)
}

class C<T> {
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    fun foo() = typeOf<List<T>>()
}

@Test
fun test_class() {
    val l = C<Int>().foo()
    assertEquals(List::class, l.classifier)
    val t = l.arguments.single().type!!.classifier
    assertTrue(t is KTypeParameter)
    assertFalse((t as KTypeParameter).isReified)
    assertEquals("T", (t as KTypeParameter).name)
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun <T> bar1() = typeOf<List<T>>()

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun <T> bar2() = typeOf<List<T>>()

class D {
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    fun <T> bar1() = typeOf<List<T>>()
}

@Test
fun test_equality() {
    val t1 = bar1<Int>().arguments.single().type!!.classifier
    val t2 = bar2<Int>().arguments.single().type!!.classifier
    val t3 = D().bar1<Int>().arguments.single().type!!.classifier
    assertNotEquals(t1, t2)
    assertNotEquals(t1, t3)
    assertNotEquals(t2, t3)
    assertEquals(t1, bar1<Int>().arguments.single().type!!.classifier)
    assertEquals(t2, bar2<Int>().arguments.single().type!!.classifier)
    assertEquals(t3, D().bar1<Int>().arguments.single().type!!.classifier)
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <reified T, R : T> reifiedUpperBound() = typeOf<List<R>>()

@Test
fun test_reifiedUpperBound() {
    val l = reifiedUpperBound<Any, Any>()
    assertEquals(List::class, l.classifier)
    val r = l.arguments.single().type!!.classifier
    assertTrue(r is KTypeParameter)
    assertFalse((r as KTypeParameter).isReified)
    assertEquals("R", (r as KTypeParameter).name)
    val t = (r as KTypeParameter).upperBounds.single().classifier
    assertTrue(t is KTypeParameter)
    assertTrue((t as KTypeParameter).isReified)
    assertEquals("T", (t as KTypeParameter).name)
}
