package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.lang.resolve.name.LabelName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.utils.emptyList
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.resolve.java.structure.JavaField
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaMethodDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.java.lazy.child
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.jet.lang.resolve.java.lazy.resolveAnnotations
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.java.lazy.hasNotNullAnnotation
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeAttributes
import org.jetbrains.jet.lang.resolve.java.lazy.hasMutableAnnotation
import org.jetbrains.kotlin.util.iif
import org.jetbrains.jet.lang.resolve.java.lazy.hasReadOnlyAnnotation
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jet.utils.Printer

public abstract class LazyJavaMemberScope(
        protected val c: LazyJavaResolverContextWithTypes,
        private val _containingDeclaration: DeclarationDescriptor
) : JetScope {
    private val allDescriptors: NotNullLazyValue<MutableCollection<DeclarationDescriptor>> = c.storageManager.createLazyValue{computeAllDescriptors()}

    override fun getContainingDeclaration() = _containingDeclaration

    // No object can be defined in Java
    override fun getObjectDescriptor(name: Name): ClassDescriptor? = null
    override fun getObjectDescriptors() = emptyList<ClassDescriptor>()

    override fun getLocalVariable(name: Name): VariableDescriptor? = null
    override fun getDeclarationsByLabel(labelName: LabelName) = emptyList<DeclarationDescriptor>()

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()
    override fun getAllDescriptors() = allDescriptors()

    private fun computeAllDescriptors(): MutableCollection<DeclarationDescriptor> {
        val result = arrayListOf<DeclarationDescriptor>()

        for (name in getAllPackageNames()) {
            val descriptor = getNamespace(name)
            result.add(descriptor ?: throw IllegalStateException("Descriptor not found for name $name in " + getContainingDeclaration()))
        }

        for (name in getAllClassNames()) {
            val descriptor = getClassifier(name)
            result.add(descriptor ?: throw IllegalStateException("Descriptor not found for name $name in " + getContainingDeclaration()))
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

    protected abstract fun getAllPackageNames(): Collection<Name>
    protected abstract fun getAllClassNames(): Collection<Name>
    protected abstract fun getAllPropertyNames(): Collection<Name>
    protected abstract fun getAllFunctionNames(): Collection<Name>
    protected abstract fun addExtraDescriptors(result: MutableCollection<in DeclarationDescriptor>)

    override fun toString() = "Lazy scope for ${getContainingDeclaration()}"
    
    TestOnly
    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("containigDeclaration: ${getContainingDeclaration()}")

        p.popIndent()
        p.println("}")
    }
}
