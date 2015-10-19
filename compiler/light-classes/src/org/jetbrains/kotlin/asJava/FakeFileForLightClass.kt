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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.stubs.PsiClassHolderFileStub
import org.jetbrains.kotlin.name.FqName

open class FakeFileForLightClass(
        private val packageFqName: FqName,
        virtualFile: VirtualFile,
        psiManager: PsiManager,
        private val lightClass: KotlinLightClass,
        private val stub: () -> PsiClassHolderFileStub<*>
) : ClsFileImpl(ClassFileViewProvider(psiManager, virtualFile)) {
    override fun getPackageName() = packageFqName.asString()

    override fun getStub() = stub()

    override fun getClasses() = arrayOf(lightClass)

    override fun getNavigationElement() = this
}