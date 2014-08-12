/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.bridges

import org.jetbrains.jet.utils.DFS
import java.util.HashSet

public trait FunctionHandle {
    public val isDeclaration: Boolean
    public val isAbstract: Boolean

    public fun getOverridden(): Iterable<FunctionHandle>
}

public data class Bridge<Signature>(
        public val from: Signature,
        public val to: Signature
) {
    override fun toString() = "$from -> $to"
}


public fun <Function : FunctionHandle, Signature> generateBridges(
        function: Function,
        signature: (Function) -> Signature
): Set<Bridge<Signature>> {
    // If it's an abstract function, no bridges are needed: when an implementation will appear in some concrete subclass, all necessary
    // bridges will be generated there
    if (function.isAbstract) return setOf()

    val fake = !function.isDeclaration

    // If it's a concrete fake override and all of its super-functions are concrete, then every possible bridge is already generated
    // into some of the super-classes and will be inherited in this class
    if (fake && function.getOverridden().none { it.isAbstract }) return setOf()

    val implementation = findConcreteSuperDeclaration(function)

    val bridgesToGenerate = findAllReachableDeclarations(function).mapTo(HashSet<Signature>(), signature)

    if (fake) {
        // If it's a concrete fake override, some of the bridges may be inherited from the super-classes. Specifically, bridges for all
        // declarations that are reachable from all concrete immediate super-functions of the given function. Note that all such bridges are
        // guaranteed to delegate to the same implementation as bridges for the given function, that's why it's safe to inherit them
        [suppress("UNCHECKED_CAST")]
        for (overridden in function.getOverridden() as Iterable<Function>) {
            if (!overridden.isAbstract) {
                bridgesToGenerate.removeAll(findAllReachableDeclarations(overridden).map(signature))
            }
        }
    }

    val method = signature(implementation)
    bridgesToGenerate.remove(method)
    return bridgesToGenerate.map { Bridge(it, method) }.toSet()
}

private fun <Function : FunctionHandle> findAllReachableDeclarations(function: Function): MutableSet<Function> {
    val collector = object : DFS.NodeHandlerWithListResult<Function, Function>() {
        override fun afterChildren(current: Function) {
            if (current.isDeclaration) {
                result.add(current)
            }
        }
    }
    [suppress("UNCHECKED_CAST")]
    DFS.dfs(listOf(function), { it.getOverridden() as Iterable<Function> }, collector)
    return HashSet(collector.result())
}

/**
 * Given a concrete function, finds an implementation (a concrete declaration) of this function in the supertypes.
 * The implementation is guaranteed to exist because if it wouldn't, the given function would've been abstract
 */
private fun <Function : FunctionHandle> findConcreteSuperDeclaration(function: Function): Function {
    require(!function.isAbstract, "Only concrete functions have implementations: $function")

    if (function.isDeclaration) return function

    // To find an implementation of a concrete fake override, we should find among all its super-functions such function that:
    // 1) it's a concrete declaration
    // 2) it's not reachable from any other declaration reachable from the given function
    // The compiler guarantees that there will be exactly one such function.
    // The following algorithm is used: first, we find all declarations reachable from the given function. Then for each such declaration
    // we remove from that result all declarations reachable from it. The result is now guaranteed to have exactly one concrete declaration
    // (and possibly some abstract declarations, which don't matter)

    val result = findAllReachableDeclarations(function)
    val toRemove = HashSet<Function>()
    for (declaration in result) {
        val reachable = findAllReachableDeclarations(declaration)
        reachable.remove(declaration)
        toRemove.addAll(reachable)
    }
    result.removeAll(toRemove)

    val concreteRelevantDeclarations = result.filter { !it.isAbstract }
    if (concreteRelevantDeclarations.size != 1) {
        error("Concrete fake override $function should have exactly one concrete super-declaration: $concreteRelevantDeclarations")
    }

    return concreteRelevantDeclarations[0]
}
