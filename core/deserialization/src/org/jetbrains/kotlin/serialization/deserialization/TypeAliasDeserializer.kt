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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId

class TypeAliasDeserializer(private val components: DeserializationComponents) {

    fun deserializeTypeAlias(typeAliasId: ClassId): TypeAliasDescriptor? {
        val parentScope = if (typeAliasId.isNestedClass) {
            val outerClass = components.classDeserializer.deserializeClass(typeAliasId.outerClassId) ?: return null
            outerClass.unsubstitutedMemberScope
        }
        else {
            val packageFragment = components.packageFragmentProvider.getPackageFragments(typeAliasId.packageFqName).single()
            packageFragment.getMemberScope()
        }

        return parentScope.getContributedClassifier(typeAliasId.shortClassName, NoLookupLocation.FROM_DESERIALIZATION) as? TypeAliasDescriptor
    }

}
