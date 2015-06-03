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

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kind
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kinds
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.storage.StorageManager
import kotlin.platform.platformStatic

/**
 * Produces descriptors representing the fictitious classes for function types, such as kotlin.Function1 or kotlin.reflect.KMemberFunction0.
 */
public class BuiltInFictitiousFunctionClassFactory(
        private val storageManager: StorageManager,
        private val module: ModuleDescriptor
) : ClassDescriptorFactory {

    private data class KindWithArity(val kind: Kind, val arity: Int)

    companion object {
        platformStatic public fun parseClassName(className: String, allowedKinds: Set<Kind>): KindWithArity? {
            for (kind in allowedKinds) {
                val prefix = kind.classNamePrefix
                if (!className.startsWith(prefix)) continue

                val arity = try {
                    className.substring(prefix.length()).toInt()
                }
                catch (e: NumberFormatException) {
                    continue
                }

                // TODO: validate arity, should be <= 255 for functions, <= 254 for members/extensions
                return KindWithArity(kind, arity)
            }

            return null
        }
    }

    override fun createClass(classId: ClassId): ClassDescriptor? {
        if (classId.isLocal() || classId.isNestedClass()) return null

        val className = classId.getRelativeClassName().asString()
        if ("Function" !in className) return null // An optimization

        val packageFqName = classId.getPackageFqName()

        val allowedKinds = when (packageFqName) {
            BUILT_INS_PACKAGE_FQ_NAME -> Kinds.Functions
            KOTLIN_REFLECT_FQ_NAME -> Kinds.KFunctions
            else -> return null
        }

        val kindWithArity = parseClassName(className, allowedKinds) ?: return null
        val (kind, arity) = kindWithArity // KT-5100

        val containingPackageFragment = module.getPackage(packageFqName)!!.getFragments().single()

        return FunctionClassDescriptor(storageManager, containingPackageFragment, kind, arity)
    }
}
