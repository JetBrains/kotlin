package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.Name
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.java.lazy.withTypes
import org.jetbrains.jet.lang.resolve.java.lazy.TypeParameterResolver
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.utils.flatten
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.util.inn
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule
import org.jetbrains.kotlin.util.sure

public abstract class LazyJavaPackageFragmentScope(
        c: LazyJavaResolverContext,
        packageFragment: LazyJavaPackageFragment
) : LazyJavaMemberScope(c.withTypes(), packageFragment) {
    
    protected val fqName: FqName = DescriptorUtils.getFqName(packageFragment).toSafe()
    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
        name ->
        val fqName = fqName.child(name)
        val javaClass = c.finder.findClass(fqName)
        if (javaClass == null)
            c.javaClassResolver.resolveClassByFqName(fqName)
        else {
            // TODO: this caching is a temporary workaround, should be replaced with properly caching the whole LazyJavaSubModule
            val cached = c.javaResolverCache.getClass(javaClass)
            if (cached != null)
                cached
            else
                LazyJavaClassDescriptor(c.withTypes(TypeParameterResolver.EMPTY), packageFragment, fqName, javaClass)
        }
    }

    protected fun computeMemberIndexForSamConstructors(delegate: MemberIndex): MemberIndex = object : MemberIndex by delegate {
        override fun getAllMethodNames(): Collection<Name> {
            val jClass = c.finder.findClass(fqName)
            return delegate.getAllMethodNames() +
                   // For SAM-constructors
                   getAllClassNames() +
                   jClass.inn({ jC -> jC.getInnerClasses().map { c -> c.getName() }}, listOf())
        }
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? = classes(name)
    public abstract override fun getAllClassNames(): Collection<Name>

    // Package fragments are not nested
    override fun getPackage(name: Name) = null
    final override fun getAllPackageNames() = listOf<Name>()
    abstract fun getSubPackages(): Collection<FqName>

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()

    override fun getContainingDeclaration()= super.getContainingDeclaration() as LazyJavaPackageFragment
}

public class LazyPackageFragmentScopeForJavaPackage(
        c: LazyJavaResolverContext,
        private val jPackage: JavaPackage,
        packageFragment: LazyJavaPackageFragment
) : LazyJavaPackageFragmentScope(c, packageFragment) {

    override fun computeMemberIndex(): MemberIndex = computeMemberIndexForSamConstructors(EMPTY_MEMBER_INDEX)

    override fun getAllClassNames(): Collection<Name> {
        return jPackage.getClasses().iterator()
                .filter { c -> c.getOriginKind() != JavaClass.OriginKind.KOTLIN_LIGHT_CLASS }
                .map { c -> c.getName() }.toList()
    }

    override fun getSubPackages(): Collection<FqName> =
            listOf(
                // We do not filter by hasStaticMembers() because it's slow (e.g. it triggers light class generation),
                // and there's no harm in having some names in the result that can not be resolved
                jPackage.getClasses().map { c -> c.getFqName().sure("Toplevel class has no fqName: $c}") },
                jPackage.getSubPackages().map { sp -> sp.getFqName() }
            ).flatten()


    override fun getProperties(name: Name): Collection<VariableDescriptor> = Collections.emptyList()
    override fun getAllPropertyNames() = Collections.emptyList<Name>()
}

public class LazyPackageFragmentScopeForJavaClass(
        c: LazyJavaResolverContext,
        private val jClass: JavaClass,
        packageFragment: LazyJavaPackageFragment
) : LazyJavaPackageFragmentScope(c, packageFragment) {

    override fun computeMemberIndex(): MemberIndex = computeMemberIndexForSamConstructors(ClassMemberIndex(jClass, { m -> m.isStatic() }))

    // nested classes are loaded as members of their outer classes, not packages
    override fun getAllClassNames(): Collection<Name> = listOf()
    override fun getClassifier(name: Name): ClassifierDescriptor? = null

    // We do not filter by hasStaticMembers() because it's slow (e.g. it triggers light class generation),
    // and there's no harm in having some names in the result that can not be resolved
    override fun getSubPackages(): Collection<FqName> = jClass.getInnerClasses().iterator()
                                                                .filter { c -> c.isStatic() }
                                                                .map { c -> c.getFqName().sure("Nested class has no fqName: $c}") }.toList()
}
