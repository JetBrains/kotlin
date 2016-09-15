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

@file:JvmName("ImportsUtils")

package org.jetbrains.kotlin.idea.imports

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

object ImportPathComparator : Comparator<ImportPath> {
    override fun compare(import1: ImportPath, import2: ImportPath): Int {
        // alias imports placed last
        if (import1.hasAlias() != import2.hasAlias()) {
            return if (import1.hasAlias()) +1 else -1
        }

        // standard library imports last
        val stdlib1 = isJavaOrKotlinStdlibImport(import1)
        val stdlib2 = isJavaOrKotlinStdlibImport(import2)
        if (stdlib1 != stdlib2) {
            return if (stdlib1) +1 else -1
        }

        return import1.toString().compareTo(import2.toString())
    }

    private fun isJavaOrKotlinStdlibImport(path: ImportPath): Boolean {
        val s = path.pathStr
        return s.startsWith("java.") || s.startsWith("javax.")|| s.startsWith("kotlin.")
    }
}

val DeclarationDescriptor.importableFqName: FqName?
    get() {
        if (!canBeReferencedViaImport()) return null
        return getImportableDescriptor().fqNameSafe
    }

fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
    if (this is PackageViewDescriptor ||
        DescriptorUtils.isTopLevelDeclaration(this) ||
        this is CallableDescriptor && DescriptorUtils.isStaticDeclaration(this)) {
        return !name.isSpecial
    }

    val parentClass = containingDeclaration as? ClassDescriptor ?: return false
    if (!parentClass.canBeReferencedViaImport()) return false

    return when (this) {
        is ConstructorDescriptor -> !parentClass.isInner // inner class constructors can't be referenced via import
        is ClassDescriptor -> true
        else -> parentClass.kind == ClassKind.OBJECT
    }
}

fun KotlinType.canBeReferencedViaImport(): Boolean {
    val descriptor = constructor.declarationDescriptor
    return descriptor != null && descriptor.canBeReferencedViaImport()
}

// for cases when class qualifier refers companion object treats it like reference to class itself
fun KtReferenceExpression.getImportableTargets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
    val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, this]?.let { listOf(it) }
                  ?: getReferenceTargets(bindingContext)
    return targets.map { it.getImportableDescriptor() }.toSet()
}

