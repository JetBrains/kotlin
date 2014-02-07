package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyPackageFragmentForJavaPackage
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyPackageFragmentForJavaClass
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPackageFragmentProvider
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass

public class LazyJavaPackageFragmentProvider(
        outerContext: GlobalJavaResolverContext,
        private val _module: ModuleDescriptor
) : JavaPackageFragmentProvider {

    private val outerClassResolver = outerContext.javaClassResolver

    private val c = LazyJavaResolverContext(
            this,
            outerContext.storageManager,
            outerContext.finder,
            outerContext.kotlinClassFinder,
            outerContext.deserializedDescriptorResolver,
            FragmentClassResolver(),
            outerContext.externalAnnotationResolver,
            outerContext.externalSignatureResolver,
            outerContext.errorReporter,
            outerContext.methodSignatureChecker,
            outerContext.javaResolverCache,
            outerContext.javaDescriptorResolver
    )

    override fun getModule() = _module

    override fun getJavaDescriptorResolver() = c.javaDescriptorResolver

    private val _packageFragments: MemoizedFunctionToNullable<FqName, LazyJavaPackageFragment> = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqName ->
        val jPackage = c.finder.findPackage(fqName)
        if (jPackage != null) {
            LazyPackageFragmentForJavaPackage(c, _module, jPackage)
        }
        else {
            val jClass = c.findJavaClass(fqName)
            if (jClass != null && DescriptorResolverUtils.isJavaClassVisibleAsPackage(jClass)) {
                LazyPackageFragmentForJavaClass(c, _module, jClass)
            }
            else null
        }
    }

    override fun getPackageFragment(fqName: FqName) = _packageFragments(fqName)

    override fun getPackageFragments(fqName: FqName) = getPackageFragment(fqName)?.let {listOf(it)}.orEmpty()

    override fun getSubPackagesOf(fqName: FqName) = getPackageFragment(fqName)?.getMemberScope()?.getSubPackages().orEmpty()

    override fun getClassNamesInPackage(packageName: FqName) = getPackageFragment(packageName)?.getMemberScope()?.getAllClassNames().orEmpty()

    fun getClass(fqName: FqName): ClassDescriptor? = c.javaClassResolver.resolveClassByFqName(fqName)

    internal val resolveKotlinBinaryClass = c.storageManager.createMemoizedFunctionWithNullableValues {
        (kotlinClass: KotlinJvmBinaryClass) -> c.deserializedDescriptorResolver.resolveClass(kotlinClass)
    }

    private inner class FragmentClassResolver : LazyJavaClassResolver {
        override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
            // TODO: there's no notion of module separation here. We must refuse to resolve classes from other modules
            val fqName = javaClass.getFqName()
            if (fqName != null) {
                // TODO: this should be handled by module separation logic
                val builtinClass = DescriptorResolverUtils.getKotlinBuiltinClassDescriptor(fqName)
                if (builtinClass != null) return builtinClass

                if (javaClass.getOriginKind() == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) {
                    return c.javaResolverCache.getClassResolvedFromSource(fqName)
                }
            }

            val outer = javaClass.getOuterClass()
            val scope = if (outer != null) {
                val outerClass = resolveClass(outer)
                if (outerClass == null) return outerClassResolver.resolveClass(javaClass)
                outerClass.getUnsubstitutedInnerClassesScope()
            }
            else {
                val outerPackage = getPackageFragment(fqName!!.parent())
                if (outerPackage == null) return outerClassResolver.resolveClass(javaClass)
                outerPackage.getMemberScope()
            }
            return scope.getClassifier(javaClass.getName()) as? ClassDescriptor
                        ?: outerClassResolver.resolveClass(javaClass)
        }

        override fun resolveClassByFqName(fqName: FqName): ClassDescriptor? {
            val builtinClass = DescriptorResolverUtils.getKotlinBuiltinClassDescriptor(fqName)
            if (builtinClass != null) return builtinClass

            // TODO Here we prefer sources (something outside JDR subsystem) to binaries, which should actually be driven by module dependencies separation
            // See DeserializedDescriptorResolver.javaDescriptorFinder
            val classFromSources = outerClassResolver.resolveClassByFqName(fqName)
            if (classFromSources != null) return classFromSources

            val (jClass, kClass) = c.findClassInJava(fqName)
            if (jClass != null) return resolveClass(jClass)
            if (kClass != null) return kClass

            return null
        }
    }
}