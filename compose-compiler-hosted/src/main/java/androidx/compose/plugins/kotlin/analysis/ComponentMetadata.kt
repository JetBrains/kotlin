package androidx.compose.plugins.kotlin.analysis

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import androidx.compose.plugins.kotlin.GeneratedViewClassDescriptor
import androidx.compose.plugins.kotlin.ComposeUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * ComponentMetadata takes in a DeclarationDescriptor and interprets it as a component.
 * All assumptions about the Component API (public and private) should reside in this class.
 *
 * A ComponentMetadata can be used to get Compose synthetic method descriptors on a component,
 * as well as descriptors for the various synthetic helper classes.
 */
class ComponentMetadata(val descriptor: ClassDescriptor) {
    // TODO(lmr): ClassDescriptor won't handle SFCs. Consider how to refactor.

    val wrapperViewDescriptor by lazy {
        GeneratedViewClassDescriptor(this)
    }

    init {
        if (!isComposeComponent(
                descriptor
            )
        )
            throw IllegalArgumentException("Not a component: " + descriptor::class)
    }

    companion object {
        // Use DeclarationDescriptor as the primary key (instead of PSI), because the PSI doesn't
        // exist for synthetic descriptors
        private val cache = HashMap<DeclarationDescriptor, ComponentMetadata>()

        fun isComponentCompanion(cls: ClassDescriptor): Boolean {
            if (!cls.isCompanionObject) return false
            if (!cls.name.identifier.contains("R4HStaticRenderCompanion")) return false
            val containingClass = cls.containingDeclaration as? ClassDescriptor ?: return false
            if (!isComposeComponent(
                    containingClass
                )
            ) return false
            return true
        }

        fun isComposeComponent(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is ClassDescriptor) return false
            val baseComponentDescriptor =
                descriptor.module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(
                        FqName(ComposeUtils.generateComposePackageName() + ".Component")
                    )
                ) ?: return false
            return descriptor.isSubclassOf(baseComponentDescriptor)
        }

        fun isWrapperView(descriptor: DeclarationDescriptor): Boolean {
            return descriptor is GeneratedViewClassDescriptor
        }

        fun fromDescriptor(descriptor: ClassDescriptor): ComponentMetadata {
            if (!cache.containsKey(descriptor)) cache[descriptor] =
                ComponentMetadata(descriptor)
            return cache[descriptor]!!
        }
    }

    fun getAttributeDescriptors(): List<PropertyDescriptor> {
        return descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            .mapNotNull { it as? PropertyDescriptor }
            .filter {
                it.containingDeclaration == descriptor && !Visibilities.isPrivate(it.visibility) &&
                        it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
            }
            .sortedBy { it.name }
    }
}