/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name

interface MetadataSource {
    val name: Name?
    val source: KtSourceElement? get() = null

    interface File : MetadataSource {
        var serializedIr: ByteArray?
    }
    interface Class : MetadataSource {
        var serializedIr: ByteArray?
    }
    interface Script : MetadataSource
    interface CodeFragment : MetadataSource
    interface Function : MetadataSource
    interface Property : MetadataSource {
        val isConst: Boolean
        val psi: PsiElement?
    }
}

sealed class DescriptorMetadataSource : MetadataSource {
    open val descriptor: Named?
        get() = null

    override val name: Name?
        get() = descriptor?.name

    class File(val descriptors: List<DeclarationDescriptor>) : DescriptorMetadataSource(), MetadataSource.File {
        override var serializedIr: ByteArray? = null
    }

    class Class(override val descriptor: ClassDescriptor) : DescriptorMetadataSource(), MetadataSource.Class {
        override var serializedIr: ByteArray? = null
    }

    class Script(override val descriptor: ScriptDescriptor) : DescriptorMetadataSource(), MetadataSource.Script

    class Function(override val descriptor: FunctionDescriptor) : DescriptorMetadataSource(), MetadataSource.Function

    class Property(override val descriptor: PropertyDescriptor) : DescriptorMetadataSource(), MetadataSource.Property {
        override val isConst: Boolean get() = descriptor.isConst
        override val psi: PsiElement? get() = null
    }

    class LocalDelegatedProperty(override val descriptor: VariableDescriptorWithAccessors) : DescriptorMetadataSource(),
        MetadataSource.Property {
        override val isConst: Boolean get() = descriptor.isConst
        override val psi: PsiElement? get() = null
    }
}
