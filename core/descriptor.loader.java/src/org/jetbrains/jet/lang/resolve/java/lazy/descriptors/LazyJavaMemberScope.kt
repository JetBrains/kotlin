package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.lang.resolve.name.LabelName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.utils.emptyList
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jet.utils.Printer

public abstract class LazyJavaMemberScope(
        private val c: LazyJavaResolverContext,
        private val _containingDeclaration: DeclarationDescriptor
) : JetScope {
    private val allDescriptors: NotNullLazyValue<MutableCollection<DeclarationDescriptor>> = c.storageManager.createLazyValue{computeAllDescriptors()}

    override fun getContainingDeclaration() = _containingDeclaration

    // No object can be defined in Java
    override fun getObjectDescriptor(name: Name): ClassDescriptor? = null
    override fun getObjectDescriptors() = emptyList<ClassDescriptor>()

    // namespaces should be resolved elsewhere
    override fun getNamespace(name: Name): NamespaceDescriptor? = null

    override fun getLocalVariable(name: Name): VariableDescriptor? = null
    override fun getDeclarationsByLabel(labelName: LabelName) = emptyList<DeclarationDescriptor>()

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()
    override fun getAllDescriptors() = allDescriptors()

    private fun computeAllDescriptors(): MutableCollection<DeclarationDescriptor> {
        val result = arrayListOf<DeclarationDescriptor>()

        for (name in getAllClassNames()) {
            val descriptor = getClassifier(name)
            assert(descriptor != null) {"Descriptor not found for name " + name + " in " + getContainingDeclaration()}
            result.add(descriptor!!)
        }

        for (name in getAllFunctionNames()) {
            result.addAll(getFunctions(name))
        }

        for (name in getAllPropertyNames()) {
            result.addAll(getProperties(name))
        }

        addExtraDescriptors(result)

        return result
    }
    protected abstract fun getAllClassNames(): Collection<Name>
    protected abstract fun getAllPropertyNames(): Collection<Name>
    protected abstract fun getAllFunctionNames(): Collection<Name>
    protected abstract fun addExtraDescriptors(result: MutableCollection<in DeclarationDescriptor>)

    TestOnly
    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("containigDeclaration: ${getContainingDeclaration()}")

        p.popIndent()
        p.println("}")
    }
}
