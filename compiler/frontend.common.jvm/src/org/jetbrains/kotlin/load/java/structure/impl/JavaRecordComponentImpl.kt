/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl

import com.intellij.psi.PsiRecordComponent
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaRecordComponent
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource
import org.jetbrains.kotlin.name.FqName

class JavaRecordComponentImpl(
    psiRecordComponentSource: JavaElementPsiSource<PsiRecordComponent>
) : JavaMemberImpl<PsiRecordComponent>(psiRecordComponentSource), JavaRecordComponent {
    override val type: JavaType
        get() = JavaTypeImpl.create(psi.type, sourceFactory.createVariableReturnTypeSource(psiElementSource))

    override val isVararg: Boolean
        get() = psi.isVarArgs

    override val annotations: List<JavaAnnotation> by lazy {
        psi.annotations.map { JavaAnnotationImpl(sourceFactory.createPsiSource(it)) }
    }

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isDeprecatedInJavaDoc: Boolean
        get() = false
}
