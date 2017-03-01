package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class KonanStringTable : StringTableImpl() {
    override fun getFqNameIndexOfLocalAnonymousClass(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        return if (descriptor.containingDeclaration is CallableMemberDescriptor) {
            val superClassifiers = descriptor.getAllSuperClassifiers()
                .mapNotNull { it as ClassifierDescriptorWithTypeParameters }
                .filter { it != descriptor }
                .toList()
            if (superClassifiers.size == 1) {
                    getFqNameIndex(superClassifiers[0])
            } else {
                val superClass = superClassifiers.find { !DescriptorUtils.isInterface(it) }
                getFqNameIndex(superClass ?: descriptor.module.builtIns.any)
            }
        } else {
            super.getFqNameIndexOfLocalAnonymousClass(descriptor)
        }
    }
}
