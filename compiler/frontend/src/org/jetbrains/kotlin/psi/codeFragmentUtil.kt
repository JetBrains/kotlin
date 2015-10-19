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

package org.jetbrains.kotlin.psi.codeFragmentUtil

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.types.KtType

public val SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE: Key<Boolean> = Key.create<Boolean>("SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE")

public var KtFile.suppressDiagnosticsInDebugMode: Boolean
    get() = when (this) {
        is KtCodeFragment -> true
        is KtFile -> getUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE) ?: false
        else -> false
    }
    set(skip: Boolean) {
        putUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE, skip)
    }

public val DEBUG_TYPE_REFERENCE_STRING: String = "DebugTypeKotlinRulezzzz"

public val DEBUG_TYPE_INFO: Key<KtType> = Key.create<KtType>("DEBUG_TYPE_INFO")
public var KtTypeReference.debugTypeInfo: KtType?
    get() = getUserData(DEBUG_TYPE_INFO)
    set(type: KtType?) {
        if (type != null && this.getText() == DEBUG_TYPE_REFERENCE_STRING) {
            putUserData(DEBUG_TYPE_INFO, type)
        }
    }
