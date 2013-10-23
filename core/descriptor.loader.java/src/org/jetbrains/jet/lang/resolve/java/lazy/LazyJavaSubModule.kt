package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyPackageFragmentForJavaPackage
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyPackageFragmentForJavaClass
import org.jetbrains.jet.lang.resolve.java.resolver.JavaNamespaceResolver

public open class LazyJavaSubModule(
        private val outerContext: GlobalJavaResolverContext,
        private val module: ModuleDescriptor
) {
    private val c = LazyJavaResolverContext(
            this,
            outerContext.storageManager,
            outerContext.finder,
            SubModuleClassResolver(),
            outerContext.externalAnnotationResolver,
            outerContext.externalSignatureResolver,
            outerContext.errorReporter,
            outerContext.methodSignatureChecker,
            outerContext.javaResolverCache
    )

    private val _packageFragments: MemoizedFunctionToNullable<FqName, NamespaceDescriptor> = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqName ->
        val jPackage = c.finder.findPackage(fqName)
        if (jPackage != null) {
            LazyPackageFragmentForJavaPackage(c, findParent(fqName), jPackage)
        }
        else {
            val jClass = c.finder.findClass(fqName)
            if (jClass != null && JavaNamespaceResolver.hasStaticMembers(jClass)) {
                LazyPackageFragmentForJavaClass(c, findParent(fqName), jClass)
            }
            else null
        }
    }

    private fun findParent(fqName: FqName) =
            if (fqName.isRoot()) module else getPackageFragment(fqName.parent())
                            ?: throw IllegalStateException("Cannot resolve parent package for: $fqName")

    fun getPackageFragment(fqName: FqName): NamespaceDescriptor? = _packageFragments(fqName)

    fun getClass(fqName: FqName): ClassDescriptor? = c.javaClassResolver.resolveClassByFqName(fqName)

    private inner class SubModuleClassResolver : LazyJavaClassResolver {
        override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
            // TODO: there's no notion of module separation here. We must refuse to resolve classes from other modules
            val outer = javaClass.getOuterClass()
            val scope = if (outer != null) {
                val outerClass = resolveClass(outer)
                if (outerClass == null) return outerContext.javaClassResolver.resolveClass(javaClass)
                outerClass.getUnsubstitutedInnerClassesScope()
            }
            else {
                val outerPackage = getPackageFragment(javaClass.getFqName()!!.parent())
                if (outerPackage == null) return outerContext.javaClassResolver.resolveClass(javaClass)
                outerPackage.getMemberScope()
            }
            return scope.getClassifier(javaClass.getName()) as? ClassDescriptor
        }

        override fun resolveClassByFqName(fqName: FqName): ClassDescriptor? {
            val jClass = c.finder.findClass(fqName)
            if (jClass == null) return null

            return resolveClass(jClass)
        }
    }
}