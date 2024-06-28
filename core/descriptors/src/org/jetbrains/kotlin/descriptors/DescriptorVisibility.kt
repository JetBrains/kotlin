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

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

abstract class DescriptorVisibility protected constructor() {
    abstract val delegate: Visibility

    val name: String
        get() = delegate.name

    val isPublicAPI: Boolean
        get() = delegate.isPublicAPI

    /**
     * @param receiver can be used to determine callee accessibility for some special receiver value
     *
     * 'null'-value basically means that receiver is absent in current call
     *
     * In case if it's needed to perform basic checks ignoring ones considering receiver (e.g. when checks happen beyond any call),
     * special value Visibilities.ALWAYS_SUITABLE_RECEIVER should be used.
     * If it's needed to determine whether visibility accepts any receiver, Visibilities.IRRELEVANT_RECEIVER should be used.
     *
     * NB: Currently Visibilities.IRRELEVANT_RECEIVER has the same effect as 'null'
     *
     * Also it's important that implementation that take receiver into account do aware about these special values.
     */
    abstract fun isVisible(
        receiver: ReceiverValue?,
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        useSpecialRulesForPrivateSealedConstructors: Boolean
    ): Boolean

    /**
     * True, if it makes sense to check this visibility in imports and not import inaccessible declarations with such visibility.
     * Hint: return true, if this visibility can be checked on file's level.
     * Examples:
     * it returns false for PROTECTED because protected members of classes can be imported to be used in subclasses of their containers,
     * so when we are looking at the import, we don't know whether it is legal somewhere in this file or not.
     * it returns true for INTERNAL, because an internal declaration is either visible everywhere in a file, or invisible everywhere in the same file.
     * it returns true for PRIVATE, because there's no point in importing privates: they are inaccessible unless their short name is
     * already available without an import
     */
    abstract fun mustCheckInImports(): Boolean

    /**
     * @return null if the answer is unknown
     */
    fun compareTo(visibility: DescriptorVisibility): Int? {
        return delegate.compareTo(visibility.delegate)
    }

    // internal representation for descriptors
    abstract val internalDisplayName: String

    // external representation for diagnostics
    abstract val externalDisplayName: String

    final override fun toString(): String = delegate.toString()

    abstract fun normalize(): DescriptorVisibility

    // Should be overloaded in Java visibilities
    fun customEffectiveVisibility(): EffectiveVisibility? = delegate.customEffectiveVisibility()

    open fun visibleFromPackage(fromPackage: FqName, myPackage: FqName): Boolean = true
}

abstract class DelegatedDescriptorVisibility(override val delegate: Visibility) : DescriptorVisibility() {
    override fun mustCheckInImports(): Boolean {
        return delegate.mustCheckInImports()
    }

    // internal representation for descriptors
    override val internalDisplayName: String
        get() = delegate.internalDisplayName

    // external representation for diagnostics
    override val externalDisplayName: String
        get() = delegate.externalDisplayName

    override fun normalize(): DescriptorVisibility = DescriptorVisibilities.toDescriptorVisibility(delegate.normalize())
}
