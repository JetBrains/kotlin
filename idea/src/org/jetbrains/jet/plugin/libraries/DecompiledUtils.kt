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

package org.jetbrains.jet.plugin.libraries

import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.resolve.kotlin.KotlinBinaryClassCache
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader.Kind
import com.intellij.psi.ClassFileViewProvider


//TODO: this should be done via generic mechanism (special header kind)
//TODO: should also check for local classes and functions
public fun isAnonymousFunction(file: VirtualFile): Boolean {
    val name = file.getNameWithoutExtension()
    val index = name.lastIndexOf('$', name.length())
    if (index > 0 && index < name.length() - 1) {
        val nameAfterBucks = name.substring(index + 1, name.size)
        return nameAfterBucks.isNotEmpty() && nameAfterBucks[0].isDigit()
    }
    return false
}

public fun isKotlinCompiledFile(file: VirtualFile): Boolean {
    if (!StdFileTypes.CLASS.getDefaultExtension().equals(file.getExtension())) {
        return false
    }
    if (isAnonymousFunction(file)) {
        return true
    }
    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file).getClassHeader()
    return header != null
}

public fun isKotlinInternalCompiledFile(file: VirtualFile): Boolean {
    if (!isKotlinCompiledFile(file)) {
        return false
    }
    if (ClassFileViewProvider.isInnerClass(file)) {
        return true
    }
    if (isAnonymousFunction(file)) {
        return true
    }
    val header = KotlinBinaryClassCache.getKotlinBinaryClass(file).getClassHeader()
    return header != null && header.getKind() == KotlinClassHeader.Kind.PACKAGE_FRAGMENT
}