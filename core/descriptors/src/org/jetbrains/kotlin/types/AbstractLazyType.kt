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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.annotations.Annotations

public abstract class AbstractLazyType(storageManager: StorageManager) : AbstractJetType(), LazyType {

    private val typeConstructor = storageManager.createLazyValue { computeTypeConstructor() }
    override fun getConstructor(): TypeConstructor = typeConstructor()

    protected abstract fun computeTypeConstructor(): TypeConstructor

    private val arguments = storageManager.createLazyValue { computeArguments() }
    override fun getArguments(): List<TypeProjection> = arguments()

    protected abstract fun computeArguments(): List<TypeProjection>

    private val memberScope = storageManager.createLazyValue { computeMemberScope() }
    override fun getMemberScope() = memberScope()

    protected abstract fun computeMemberScope(): JetScope

    override fun isMarkedNullable() = false

    override fun isError() = getConstructor().getDeclarationDescriptor()?.let { d -> ErrorUtils.isError(d) } ?: false

    override fun getAnnotations() = Annotations.EMPTY

    override fun toString(): String {
        if (!typeConstructor.isComputed()) {
            return "Type constructor is not computed"
        }
        if (!arguments.isComputed()) {
            return "" + getConstructor() + "<arguments are not computed>"
        }
        return super<AbstractJetType>.toString()
    }
}
