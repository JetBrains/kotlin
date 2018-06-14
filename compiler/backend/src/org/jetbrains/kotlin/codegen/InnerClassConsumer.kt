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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.util.*

interface InnerClassConsumer {
    fun addInnerClassInfoFromAnnotation(classDescriptor: ClassDescriptor)

    companion object {

        fun classForInnerClassRecord(descriptor: ClassDescriptor, defaultImpls: Boolean): ClassDescriptor? {
            if (defaultImpls) {
                if (DescriptorUtils.isLocal(descriptor)) return null
                val classDescriptorImpl = ClassDescriptorImpl(
                    descriptor, Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME),
                    Modality.FINAL, ClassKind.CLASS, Collections.emptyList(), SourceElement.NO_SOURCE,
                    /* isExternal = */ false, LockBasedStorageManager.NO_LOCKS
                )

                classDescriptorImpl.initialize(MemberScope.Empty, emptySet(), null)
                return classDescriptorImpl
            } else {
                return if (DescriptorUtils.isTopLevelDeclaration(descriptor)) null else descriptor
            }
        }
    }
}