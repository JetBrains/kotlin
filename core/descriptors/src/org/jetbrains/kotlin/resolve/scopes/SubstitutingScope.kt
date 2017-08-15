/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Substitutable
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.wrapWithCapturingSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize
import org.jetbrains.kotlin.utils.sure
import java.util.*

class SubstitutingScope(private val workerScope: MemberScope, givenSubstitutor: TypeSubstitutor) : MemberScope {

    private val substitutor = givenSubstitutor.substitution.wrapWithCapturingSubstitution().buildSubstitutor()

    private var substitutedDescriptors: MutableMap<DeclarationDescriptor, DeclarationDescriptor>? = null

    private val _allDescriptors by lazy { substitute(workerScope.getContributedDescriptors()) }

    private fun <D : DeclarationDescriptor> substitute(descriptor: D): D {
        if (substitutor.isEmpty) return descriptor

        if (substitutedDescriptors == null) {
            substitutedDescriptors = HashMap<DeclarationDescriptor, DeclarationDescriptor>()
        }

        val substituted = substitutedDescriptors!!.getOrPut(descriptor) {
            when (descriptor) {
                is Substitutable<*> -> descriptor.substitute(substitutor).sure {
                    "We expect that no conflict should happen while substitution is guaranteed to generate invariant projection, " +
                    "but $descriptor substitution fails"
                }
                else -> error("Unknown descriptor in scope: $descriptor")
            }
        }

        @Suppress("UNCHECKED_CAST")
        return substituted as D
    }

    private fun <D : DeclarationDescriptor> substitute(descriptors: Collection<D>): Collection<D> {
        if (substitutor.isEmpty) return descriptors
        if (descriptors.isEmpty()) return descriptors

        val result = newHashSetWithExpectedSize<D>(descriptors.size)
        for (descriptor in descriptors) {
            val substitute = substitute(descriptor)
            result.add(substitute)
        }

        return result
    }

    override fun getContributedVariables(name: Name, location: LookupLocation) = substitute(workerScope.getContributedVariables(name, location))

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
            workerScope.getContributedClassifier(name, location)?.let { substitute(it) }

    override fun getContributedFunctions(name: Name, location: LookupLocation) = substitute(workerScope.getContributedFunctions(name, location))

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean) = _allDescriptors

    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()

    override fun definitelyDoesNotContainName(name: Name) = workerScope.definitelyDoesNotContainName(name)

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("substitutor = ")
        p.pushIndent()
        p.println(substitutor)
        p.popIndent()

        p.print("workerScope = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
