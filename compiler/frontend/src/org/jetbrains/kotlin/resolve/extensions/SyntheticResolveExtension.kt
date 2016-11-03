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

package org.jetbrains.kotlin.resolve.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.*

//----------------------------------------------------------------
// extension interface

interface SyntheticResolveExtension {
    companion object : ProjectExtensionDescriptor<SyntheticResolveExtension>(
            "org.jetbrains.kotlin.syntheticResolveExtension", SyntheticResolveExtension::class.java) {
        fun getInstance(project: Project): SyntheticResolveExtension {
            val instances = getInstances(project)
            if (instances.size == 1) return instances.single()
            // return list combiner here
            return object : SyntheticResolveExtension {
                override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? =
                    instances.firstNotNullResult { it.getSyntheticCompanionObjectNameIfNeeded(thisDescriptor) }

                override fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) =
                    instances.forEach { it.addSyntheticSupertypes(thisDescriptor, supertypes) }

                override fun generateSyntheticMethods(thisDescriptor: ClassDescriptor, name: Name,
                                                      fromSupertypes: List<SimpleFunctionDescriptor>,
                                                      result: MutableCollection<SimpleFunctionDescriptor>) =
                    instances.forEach { it.generateSyntheticMethods(thisDescriptor, name, fromSupertypes, result) }

                override fun generateSyntheticProperties(thisDescriptor: ClassDescriptor, name: Name,
                                                         fromSupertypes: ArrayList<PropertyDescriptor>,
                                                         result: MutableSet<PropertyDescriptor>)  =
                    instances.forEach { it.generateSyntheticProperties(thisDescriptor, name, fromSupertypes, result) }
            }
        }
    }

    fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name?

    fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {}

    fun generateSyntheticMethods(thisDescriptor: ClassDescriptor,
                                 name: Name,
                                 fromSupertypes: List<SimpleFunctionDescriptor>,
                                 result: MutableCollection<SimpleFunctionDescriptor>) {}

    fun generateSyntheticProperties(thisDescriptor: ClassDescriptor,
                                    name: Name,
                                    fromSupertypes:
                                    ArrayList<PropertyDescriptor>,
                                    result: MutableSet<PropertyDescriptor>) {}
}
