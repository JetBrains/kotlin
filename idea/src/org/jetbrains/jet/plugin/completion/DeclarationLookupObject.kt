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

package org.jetbrains.jet.plugin.completion

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for sorting
 */
public class DeclarationLookupObject(public val descriptor: DeclarationDescriptor?, private val analyzer: KotlinCodeAnalyzer, public val psiElement: PsiElement?) {
    override fun toString(): String {
        return super.toString() + " " + descriptor
    }

    override fun hashCode(): Int {
        return if (descriptor != null) descriptor!!.hashCode() else 0
    }

    override fun equals(other: Any?): Boolean {
        if (this identityEquals other) return true
        if (other == null || javaClass != other.javaClass) return false

        val lookupObject = other as DeclarationLookupObject

        if (analyzer != lookupObject.analyzer) {
            LOG.warn("Descriptors from different resolve sessions")
            return false
        }

        return lookupObject.descriptor == descriptor
    }

    class object {
        private val LOG = Logger.getInstance("#" + javaClass<DeclarationLookupObject>().getName())
    }
}
