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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.serialization.deserialization.AdditionalSupertypes
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.DelegatingType
import org.jetbrains.kotlin.types.KtType
import java.io.Serializable

class BuiltInClassesAreSerializableOnJvm(
        private val moduleDescriptor: ModuleDescriptor
) : AdditionalSupertypes {

    private val mockSerializableType = createMockJavaIoSerializableType()

    private fun createMockJavaIoSerializableType(): KtType {
        val mockJavaIoPackageFragment = object : PackageFragmentDescriptorImpl(moduleDescriptor, FqName("java.io")) {
            override fun getMemberScope() = KtScope.Empty
        }

        //NOTE: can't reference anyType right away, because this is sometimes called when JvmBuiltIns are initializing
        val superTypes = listOf(object : DelegatingType() {
            override fun getDelegate(): KtType {
                return JvmBuiltIns.Instance.anyType
            }
        })

        val mockSerializableClass = ClassDescriptorImpl(
                mockJavaIoPackageFragment, Name.identifier("Serializable"), Modality.ABSTRACT, superTypes, SourceElement.NO_SOURCE
        )

        mockSerializableClass.initialize(KtScope.Empty, emptySet(), null)
        return mockSerializableClass.defaultType
    }

    override fun forClass(classDescriptor: DeserializedClassDescriptor): Collection<KtType> {
        if (isSerializableInJava(classDescriptor.fqNameSafe)) {
            return listOf(mockSerializableType)
        }
        else return listOf()
    }

    private fun isSerializableInJava(classFqName: FqName): Boolean {
        val fqNameUnsafe = classFqName.toUnsafe()
        if (fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.array || KotlinBuiltIns.isPrimitiveArray(fqNameUnsafe)) {
            return true
        }
        val javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(fqNameUnsafe) ?: return false
        val classViaReflection = try {
            Class.forName(javaClassId.asSingleFqName().asString())
        }
        catch (e: ClassNotFoundException) {
            return false
        }
        return Serializable::class.java.isAssignableFrom(classViaReflection)
    }
}