package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.CloneableClassScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

/**
 * Note: this class is copy-pasted from [org.jetbrains.kotlin.builtins.JvmBuiltInClassDescriptorFactory].
 */
class KonanBuiltInClassDescriptorFactory(
        storageManager: StorageManager,
        private val moduleDescriptor: ModuleDescriptor,
        private val computeContainingDeclaration: (ModuleDescriptor) -> DeclarationDescriptor = { module ->
            module.getPackage(KOTLIN_FQ_NAME).fragments.filterIsInstance<BuiltInsPackageFragment>().first()
        }
) : ClassDescriptorFactory {

    /**
     * Workaround for `kotlin.Cloneable` deserialization: it is manually blacklisted in `ClassDeserializer`,
     * so apply the same approach as on JVM, i.e. create the descriptor synthetically on demand:
     */
    private val cloneable by storageManager.createLazyValue {
        ClassDescriptorImpl(
                computeContainingDeclaration(moduleDescriptor),
                CLONEABLE_NAME, Modality.ABSTRACT, ClassKind.INTERFACE, listOf(moduleDescriptor.builtIns.anyType),
                SourceElement.NO_SOURCE, /* isExternal = */ false
        ).apply {
            initialize(CloneableClassScope(storageManager, this), emptySet(), null)
        }
    }

    override fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean =
            name == CLONEABLE_NAME && packageFqName == KOTLIN_FQ_NAME

    override fun createClass(classId: ClassId): ClassDescriptor? =
            when (classId) {
                CLONEABLE_CLASS_ID -> cloneable
                else -> null
            }

    override fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor> =
            when (packageFqName) {
                KOTLIN_FQ_NAME -> setOf(cloneable)
                else -> emptySet()
            }

    companion object {
        private val KOTLIN_FQ_NAME = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
        private val CLONEABLE_NAME = KotlinBuiltIns.FQ_NAMES.cloneable.shortName()
        val CLONEABLE_CLASS_ID = ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.cloneable.toSafe())
    }
}
