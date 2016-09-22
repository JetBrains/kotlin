/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.bridges.Bridge
import org.jetbrains.kotlin.backend.common.bridges.FunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.generateBridges
import org.jetbrains.kotlin.utils.DFS
import java.util.*
import kotlin.test.assertEquals

class BridgeTest : TestCase() {
    private class Fun(val text: String) : FunctionHandle {
        override val isDeclaration: Boolean get() = text[1] == 'D'
        override val isAbstract: Boolean get() = text[0] == '-'
        override val isInterfaceDeclaration: Boolean get() = false
        val signature: Char get() = text[2]

        val overriddenFunctions: MutableList<Fun> = arrayListOf()
        override fun getOverridden() = overriddenFunctions

        override fun toString() = text
    }

    private class Meth(val function: Fun) {
        override fun equals(other: Any?) = other is Meth && other.function.signature == function.signature
        override fun hashCode() = function.signature.hashCode()
        override fun toString() = function.toString()
    }

    private fun v(text: String): Fun {
        assert(text.length == 3) { "Function vertex representation should consist of 3 characters: $text" }
        assert(text[0] in setOf('-', '+')) { "First character should be '-' for abstract functions or '+' for concrete ones: $text" }
        assert(text[1] in setOf('D', 'F')) { "Second character should be 'D' for declarations or 'F' for fake overrides: $text" }
        assert(text[2].isDigit()) {
            "Third character should be a number that represents a signature (same numbers mean the same method signatures)"
        }
        return Fun(text)
    }

    private fun bridge(from: Fun, to: Fun): Bridge<Meth> = Bridge(Meth(from), Meth(to))

    /**
     * Constructs a graph out of the given pairs of vertices. First vertex should be a function in the derived class,
     * second -- the corresponding overridden function in a superclass.
     *
     * Checks that the graph satisfies the following conditions:
     * 1. Each fake override should have a super-declaration
     * 2. Each concrete fake override should have exactly one concrete super-declaration. More accurately, for each concrete
     *    fake override F there is a concrete declaration D in supertypes such that every other concrete super-declaration of F
     *    is either reachable from D or is reachable from any abstract super-declaration of F (or both). This condition is effectively
     *    equivalent to the compiler guarantee that each class inherits not more than one implementation of each function.
     *
     * NOTE: the graph validation procedure probably doesn't cover all the possible cases compared to the analogous code in the compiler.
     *         There may be bugs here and they should be fixed accordingly.
     *
     * TODO: also verify that no abstract fake override has a concrete super-declaration.
     *       This was previously possible via traits with required classes.
     */
    private fun graph(vararg edges: Pair<Fun, Fun>) {
        for ((from, to) in edges) {
            from.overriddenFunctions.add(to)
        }

        fun findAllReachableDeclarations(from: Fun): MutableSet<Fun> {
            val handler = object : DFS.NodeHandlerWithListResult<Fun, Fun>() {
                override fun afterChildren(current: Fun) {
                    if (current.isDeclaration) {
                        result.add(current)
                    }
                }
            }
            DFS.dfs(listOf(from), { it.getOverridden() }, handler)
            val result = HashSet(handler.result())
            result.remove(from)
            return result
        }

        val vertices = edges.flatMapTo(HashSet<Fun>()) { pair -> listOf(pair.first, pair.second) }

        for (vertex in vertices) {
            val directConcreteSuperFunctions = vertex.overriddenFunctions.filter { !it.isAbstract }
            assert(directConcreteSuperFunctions.size <= 1) {
                "Incorrect test data: function $vertex has more than one direct concrete super-function: ${vertex.overriddenFunctions}\n" +
                "This is not allowed because only classes can contain implementations (concrete functions), and having more than one " +
                "concrete super-function means having more than one superclass, which is prohibited in Kotlin"
            }

            if (vertex.isDeclaration) continue

            val superDeclarations = findAllReachableDeclarations(vertex)
            assert(superDeclarations.isNotEmpty()) { "Incorrect test data: fake override vertex $vertex has no super-declarations" }

            // Remove all declarations inherited by other declarations
            val toRemove = HashSet<Fun>()
            for (superDeclaration in superDeclarations) {
                toRemove.addAll(findAllReachableDeclarations(superDeclaration))
            }
            superDeclarations.removeAll(toRemove)
            val concreteDeclarations = superDeclarations.filter { !it.isAbstract }

            if (!vertex.isAbstract) {
                assert(concreteDeclarations.isNotEmpty()) {
                    "Incorrect test data: concrete fake override vertex $vertex has no concrete super-declarations"
                }
                assert(concreteDeclarations.size == 1) {
                    "Incorrect test data: concrete fake override vertex $vertex has more than one concrete super-declaration: " +
                    "$concreteDeclarations"
                }
            }
        }
    }

    private fun doTest(function: Fun, expectedBridges: Set<Bridge<Meth>>) {
        val actualBridges = generateBridges(function, ::Meth)
        assert(actualBridges.firstOrNull { it.from == it.to } == null) {
            "A bridge invoking itself was generated, which makes no sense, since it will result in StackOverflowError" +
            " once called: $actualBridges"
        }
        assertEquals(expectedBridges, actualBridges, "Expected and actual bridge sets differ for function $function")
    }

    // ------------------------------------------------------------------------------------------------------------------

    // Simple tests with no bridges

    fun testOneVertexAbstract() {
        val a = v("-D1")
        graph()
        doTest(a, setOf())
    }

    fun testOneVertexConcrete() {
        val a = v("+D1")
        graph()
        doTest(a, setOf())
    }
    
    fun testSimpleFakeOverrideSameSignature() {
        val a = v("+D1")
        val b = v("+F1")
        graph(b to a)
        doTest(a, setOf())
        doTest(b, setOf())
    }

    fun testSimpleFakeOverrideDifferentSignature() {
        val a = v("+D1")
        val b = v("+F2")
        graph(b to a)
        doTest(a, setOf())
        doTest(b, setOf())
    }

    fun testSimpleDeclarationSameSignature() {
        val a = v("+D1")
        val b = v("+D1")
        graph(b to a)
        doTest(a, setOf())
        doTest(b, setOf())
    }

    fun testSimpleAbstractDeclarationSameSignature() {
        val a = v("-D1")
        val b = v("-D1")
        graph(b to a)
        doTest(a, setOf())
        doTest(b, setOf())
    }

    fun testSimpleAbstractDeclarationOverridesConcreteSameSignature() {
        val a = v("+D1")
        val b = v("-D1")
        graph(b to a)
        doTest(a, setOf())
        doTest(b, setOf())
    }
    
    // Simple tests where declaration "a" is inherited by declaration "b" with a different signature.
    // Note that we don't generate bridges near abstract declarations in contrast to javac

    fun testSimpleConcreteDeclarationDifferentSignature() {
        val a = v("+D1")
        val b = v("+D2")
        graph(b to a)
        doTest(b, setOf(bridge(a, b)))
    }
    
    fun testSimpleAbstractDeclarationDifferentSignature() {
        val a = v("-D1")
        val b = v("-D2")
        graph(b to a)
        doTest(b, setOf())
    }

    fun testSimpleAbstractDeclarationOverridesConcreteDifferentSignature() {
        val a = v("+D1")
        val b = v("-D2")
        graph(b to a)
        doTest(b, setOf())
    }

    fun testSimpleConcreteDeclarationOverridesAbstractDifferentSignature() {
        val a = v("-D1")
        val b = v("+D2")
        graph(b to a)
        doTest(b, setOf(bridge(a, b)))
    }
    
    // Simple tests where declaration overrides declaration through a fake override in the super class, with a different signature

    fun testSimpleConcreteDeclarationOverridesConcreteThroughFakeOverride() {
        val a = v("+D1")
        val b = v("+F2")
        val c = v("+D3")
        graph(b to a, c to b)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf(bridge(a, c)))
    }

    fun testSimpleConcreteDeclarationOverridesAbstractThroughFakeOverride() {
        val a = v("-D1")
        val b = v("-F2")
        val c = v("+D3")
        graph(b to a, c to b)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf(bridge(a, c)))
    }

    fun testSimpleAbstractDeclarationOverridesConcreteThroughFakeOverride() {
        val a = v("+D1")
        val b = v("+F2")
        val c = v("-D3")
        graph(b to a, c to b)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf())
    }

    fun testSimpleAbstractDeclarationOverridesAbstractThroughFakeOverride() {
        val a = v("-D1")
        val b = v("-F2")
        val c = v("-D3")
        graph(b to a, c to b)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf())
    }
    
    // Declaration "c" overrides two declarations "a" and "b"

    fun testAbstractDeclarationOverridesTwoAbstractDeclarations() {
        val a = v("-D1")
        val b = v("-D2")
        val c = v("-D3")
        graph(c to a, c to b)
        doTest(c, setOf())
    }

    fun testAbstractDeclarationOverridesAbstractAndConcreteDeclarations() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("-D3")
        graph(c to a, c to b)
        doTest(c, setOf())
    }

    fun testConcreteDeclarationOverridesTwoAbstractDeclarations() {
        val a = v("-D1")
        val b = v("-D2")
        val c = v("+D3")
        graph(c to a, c to b)
        doTest(c, setOf(bridge(a, c), bridge(b, c)))
    }

    fun testConcreteDeclarationOverridesAbstractAndConcreteDeclarations() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("+D3")
        graph(c to a, c to b)
        doTest(c, setOf(bridge(a, c), bridge(b, c)))
    }

    // Declaration "c" overrides declaration "a" and declaration "b", which in turn overrides "a".
    // We still need to generate both bridges near "c" to avoid several consecutive bridges in a call stack

    fun testConcreteDeclarationOverridesTwoInheritingDeclarations() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("+D3")
        graph(c to a, c to b, b to a)
        doTest(a, setOf())
        doTest(b, setOf(bridge(a, b)))
        doTest(c, setOf(bridge(a, c), bridge(b, c)))
    }

    fun testConcreteDeclarationOverridesAbstractDeclarationOverridingConcrete() {
        val a = v("+D1")
        val b = v("-D2")
        val c = v("+D3")
        graph(c to a, c to b, b to a)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf(bridge(a, c), bridge(b, c)))
    }
    
    // Diamonds where the sink (vertex "d") is a declaration: bridges from all super-declarations to "d" should be present

    fun testDiamondAbstractDeclarations() {
        val a = v("-D1")
        val b = v("-D2")
        val c = v("-D3")
        val d = v("-D4")
        graph(b to a, c to a, d to b, d to c)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf())
        doTest(d, setOf())
    }
    
    fun testDiamondMixedDeclarations() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("-D3")
        val d = v("+D4")
        graph(b to a, c to a, d to b, d to c)
        doTest(a, setOf())
        doTest(b, setOf(bridge(a, b)))
        doTest(c, setOf())
        doTest(d, setOf(bridge(a, d), bridge(b, d), bridge(c, d)))
    }
    
    fun testDiamondAbstractFakeOverridesInTheMiddle() {
        val a = v("-D1")
        val b = v("-F2")
        val c = v("-F3")
        val d = v("+D4")
        graph(b to a, c to a, d to b, d to c)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf())
        doTest(d, setOf(bridge(a, d)))
    }

    // Fake override "c" overrides declarations "a" and "b": a bridge is needed if signatures are different and there's an implementation
    
    fun testAbstractFakeOverride() {
        val a = v("-D1")
        val b = v("-D2")
        val c = v("-F3")
        graph(c to a, c to b)
        doTest(c, setOf())
    }

    fun testFakeOverrideSameSuperDeclarations() {
        val a = v("-D1")
        val b = v("+D1")
        val c = v("+F2")
        graph(c to a, c to b)
        doTest(c, setOf())
    }

    fun testFakeOverrideAbstractAndConcreteDeclarations() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("+F3")
        graph(c to a, c to b)
        doTest(c, setOf(bridge(a, b)))
    }
    
    fun testFakeOverrideInheritingDeclarations() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("+F3")
        graph(c to a, c to b, b to a)
        doTest(a, setOf())
        doTest(b, setOf(bridge(a, b)))
        // Here bridge a->b is not needed in c, it's already present in b
        doTest(c, setOf())
    }

    // Diamonds where the sink (vertex "d") is a fake override

    fun testDiamondFakeOverrideAbstractFakeAndConcrete() {
        val a = v("-D1")
        val b = v("-F2")
        val c = v("+D3")
        val d = v("+F4")
        graph(b to a, c to a, d to b, d to c)
        doTest(c, setOf(bridge(a, c)))
        doTest(d, setOf())
    }

    fun testDiamondFakeOverrideAbstractAndConcrete() {
        val a = v("-D1")
        val b = v("-D2")
        val c = v("+D3")
        val d = v("+F4")
        graph(b to a, c to a, d to b, d to c)
        doTest(b, setOf())
        doTest(c, setOf(bridge(a, c)))
        doTest(d, setOf(bridge(b, c)))
    }

    fun testDiamondFakeOverrideAbstractOverridesConcrete() {
        val a = v("+D1")
        val b = v("+D2")
        val c = v("-D3")
        val d = v("+F4")
        graph(b to a, c to a, d to b, d to c)
        doTest(b, setOf(bridge(a, b)))
        doTest(c, setOf())
        doTest(d, setOf(bridge(c, b)))
    }

    // More complex tests where a fake override inherits several declarations

    fun testFakeOverrideInheritingDeclarationsAndAbstract() {
        val a = v("-D1")
        val b = v("-D2")
        val c = v("+D3")
        val e = v("+F4")
        graph(e to a, e to b, e to c, c to b)
        doTest(a, setOf())
        doTest(b, setOf())
        doTest(c, setOf(bridge(b, c)))
        doTest(e, setOf(bridge(a, c)))
    }

    fun testFakeOverrideManyDeclarations() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("-D3")
        val d = v("-D4")
        val e = v("+F5")
        graph(e to a, e to b, e to c, e to d)
        doTest(e, setOf(bridge(a, b), bridge(c, b), bridge(d, b)))
    }

    fun testFakeOverrideTwoDeclarationsThroughFakeOverrides() {
        val a = v("+D1")
        val b = v("+F2")
        val c = v("-D3")
        val d = v("-F4")
        val e = v("+F5")
        graph(b to a, d to c, e to b, e to d)
        doTest(e, setOf(bridge(c, a)))
    }

    fun testFakeOverrideMisleadingImplementation() {
        val a = v("+D1")
        val b = v("-D2")
        val c = v("-D3")
        val d = v("+D4")
        val e = v("+F5")
        val f = v("+F6")
        graph(c to a, e to d, f to b, f to c, f to e)
        doTest(e, setOf())
        // Although "a" is a concrete declaration, it's overridden with abstract in "c" and all bridges should delegate to "d" instead
        doTest(f, setOf(bridge(a, d), bridge(b, d), bridge(c, d)))
    }

    // Fake override overrides another fake override (or declaration) with some bridges already present there

    fun testFakeOverrideInheritsBridgeFromFakeOverride() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("+F3")
        val d = v("+F4")
        graph(c to a, c to b, d to c)
        doTest(c, setOf(bridge(a, b)))
        doTest(d, setOf())
    }

    fun testFakeOverrideInheritsBridgesAndAbstract() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("+F3")
        val d = v("-D4")
        val e = v("+F5")
        graph(c to a, c to b, e to c, e to d)
        doTest(c, setOf(bridge(a, b)))
        // It's important that "e" shouldn't have "a->b" bridge, because it's inherited from "c"
        doTest(e, setOf(bridge(d, b)))
    }

    fun testFakeOverrideInheritsBridgeFromDeclaration() {
        val a = v("+D1")
        val b = v("-D2")
        val c = v("+F3")
        val d = v("+D4")
        val e = v("-D5")
        val f = v("+F6")
        graph(c to a, c to b, d to c, f to d, f to e)
        doTest(c, setOf(bridge(b, a)))
        doTest(d, setOf(bridge(a, d), bridge(b, d)))
        doTest(f, setOf(bridge(e, d)))
    }

    fun testFakeOverrideDiamondWithExtraAbstract() {
        val a = v("+D1")
        val b = v("-F2")
        val c = v("+D3")
        val d = v("-D4")
        val e = v("+F5")
        graph(b to a, c to a, e to b, e to c, e to d)
        doTest(c, setOf(bridge(a, c)))
        // It's important that implementation of "e" is "c", not "a", so a bridge "d->c" should exist
        doTest(e, setOf(bridge(d, c)))
    }

    fun testLongTreeOfFakeOverrideBridgeInheritance() {
        val a = v("+D1")
        val b = v("-D2")
        val c = v("-D3")
        val d = v("+F4")
        val e = v("-D5")
        val f = v("-D6")
        val g = v("+F7")
        val h = v("-D8")
        val i = v("-D9")
        val j = v("+F0")
        graph(d to a, d to b, d to c,
              g to d, g to e, g to f,
              j to g, j to h, j to i)
        doTest(d, setOf(bridge(b, a), bridge(c, a)))
        doTest(g, setOf(bridge(e, a), bridge(f, a)))
        doTest(j, setOf(bridge(h, a), bridge(i, a)))
    }

    fun testFakeOverrideShouldNotInheritBridgeFromAbstractDeclaration() {
        val a = v("-D1")
        val b = v("+D2")
        val c = v("+F3")
        val d = v("-D4")
        val e = v("+D5")
        val f = v("+F6")
        graph(c to a, c to b, d to c, f to d, f to e)
        doTest(c, setOf(bridge(a, b)))
        // Although "f" has a concrete fake override "c" in its hierarchy, we should NOT silently inherit a bridge from it,
        // because it corresponds to another implementation ("b"). Instead a bunch of new bridges should be generated, delegating to "e"
        doTest(f, setOf(bridge(a, e), bridge(b, e), bridge(d, e)))
    }

    fun testFakeOverrideShouldNotInheritBridgeFromAbstractFakeOverride() {
        val a = v("+D1")
        val b = v("-D2")
        val c = v("-F3")
        val d = v("+F4")
        graph(c to a, c to b, d to a, d to c)
        doTest(c, setOf())
        doTest(d, setOf(bridge(b, a)))
    }
}
