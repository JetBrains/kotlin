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

package org.jetbrains.jet.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.psi.JetFile

public enum class MemberKind { FIELD; METHOD }

public enum class JvmDeclarationOriginKind {
    OTHER
    PACKAGE_FACADE
    PACKAGE_PART
}

public class JvmDeclarationOrigin(
        val originKind: JvmDeclarationOriginKind,
        val element: PsiElement?,
        val descriptor: DeclarationDescriptor?
) {
    class object {
        public val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, null)
    }
}

public fun OtherOrigin(element: PsiElement?, descriptor: DeclarationDescriptor?): JvmDeclarationOrigin =
        if (element == null && descriptor == null)
            JvmDeclarationOrigin.NO_ORIGIN
        else JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, element, descriptor)

public fun OtherOrigin(element: PsiElement?): JvmDeclarationOrigin = OtherOrigin(element, null)

public fun OtherOrigin(descriptor: DeclarationDescriptor?): JvmDeclarationOrigin = OtherOrigin(null, descriptor)

public fun PackageFacade(descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.PACKAGE_FACADE, null, descriptor)
public fun PackagePart(file: JetFile, descriptor: PackageFragmentDescriptor): JvmDeclarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.PACKAGE_PART, file, descriptor)

