package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.Name
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.java.lazy.withTypes
import org.jetbrains.jet.lang.resolve.java.lazy.TypeParameterResolver

public class LazyJavaPackageFragmentScope(
        private val c: LazyJavaResolverContext,
        containingDeclaration: NamespaceDescriptor
) : LazyJavaMemberScope(c.withTypes(), containingDeclaration) {
    
    private val fqName = DescriptorUtils.getFQName(containingDeclaration).toSafe()
    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
                name ->
                val fqName = fqName.child(name)
                val javaClass = c.finder.findClass(fqName)
                if (javaClass == null)
                    null
                else
                    LazyJavaClassDescriptor(c.withTypes(TypeParameterResolver.EMPTY), containingDeclaration, fqName, javaClass)
            }

    override fun getAllClassNames(): Collection<Name> {
        val javaPackage = c.finder.findPackage(fqName)
        assert(javaPackage != null) { "Package not found:  $fqName" }
        return javaPackage!!.getClasses().map { c -> c.getName() }
    }

    override fun getAllPropertyNames() = Collections.emptyList<Name>()
    override fun getAllFunctionNames() = Collections.emptyList<Name>()
    
    override fun addExtraDescriptors(result: MutableCollection<in DeclarationDescriptor>) {
        // no extra descriptors
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? = classes(name)

    // TODO
    override fun getProperties(name: Name): Collection<VariableDescriptor> = Collections.emptyList()

    // TODO
    override fun getFunctions(name: Name): Collection<FunctionDescriptor> = Collections.emptyList()

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()

}
