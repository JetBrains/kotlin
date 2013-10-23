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
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter
import org.jetbrains.jet.lang.resolve.java.resolver.JavaFunctionResolver
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import java.util.LinkedHashSet
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jet.utils.Printer

public abstract class LazyJavaMemberScope(
        protected val c: LazyJavaResolverContextWithTypes,
        private val _containingDeclaration: DeclarationDescriptor
) : JetScope {
    private val allDescriptors: NotNullLazyValue<MutableCollection<DeclarationDescriptor>> = c.storageManager.createLazyValue{computeAllDescriptors()}

    override fun getContainingDeclaration() = _containingDeclaration

    private val memberIndex = c.storageManager.createLazyValue {
        computeMemberIndex()
    }

    protected abstract fun computeMemberIndex(): MemberIndex

    private val _functions = c.storageManager.createMemoizedFunction {
        (name: Name): Collection<FunctionDescriptor>
        ->
        val methods = memberIndex().findMethodsByName(name)
        val functions = LinkedHashSet<FunctionDescriptor>(methods.map {
            method ->
            val function = JavaMethodDescriptor(
                    _containingDeclaration,
                    c.resolveAnnotations(method.getAnnotations()),
                    name
            )
            val innerC = c.child(function, method.getTypeParameters().toSet())
            val valueParameters = resolveValueParameters(innerC, function, method.getValueParameters())
            val returnTypeAttrs = LazyJavaTypeAttributes(c, method, TypeUsage.MEMBER_SIGNATURE_COVARIANT) {
                if (method.hasReadOnlyAnnotation() && !method.hasMutableAnnotation())
                    TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
                else
                    TypeUsage.MEMBER_SIGNATURE_COVARIANT
            }

            function.initialize(
                null,
                DescriptorUtils.getExpectedThisObjectIfNeeded(_containingDeclaration),
                method.getTypeParameters().map { p -> innerC.typeParameterResolver.resolveTypeParameter(p) },
                valueParameters,
                c.typeResolver.transformJavaType(method.getReturnType()!!, returnTypeAttrs),
                Modality.convertFromFlags(method.isAbstract(), !method.isFinal()),
                method.getVisibility(),
                false
            )
            function
        })

        if (_containingDeclaration is ClassDescriptor) {
            val functionsFromSupertypes = JavaFunctionResolver.getFunctionsFromSupertypes(name, _containingDeclaration);

            functions.addAll(DescriptorResolverUtils.resolveOverrides(name, functionsFromSupertypes, functions, _containingDeclaration, c.errorReporter));
        }

        // Make sure that lazy things are computed before we release the lock
        for (f in functions) {
            for (p in f.getValueParameters()) {
                p.hasDefaultValue()
            }
        }

        functions
    }

    protected fun resolveValueParameters(
            c: LazyJavaResolverContextWithTypes,
            function: FunctionDescriptor,
            jValueParameters: List<JavaValueParameter>
    ): List<ValueParameterDescriptor> {
        return jValueParameters.withIndices().map {
            pair ->
            val (index, javaParameter) = pair

            val typeUsage = LazyJavaTypeAttributes(c, javaParameter, TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT) {
                    javaParameter.hasMutableAnnotation().iif(TypeUsage.MEMBER_SIGNATURE_COVARIANT, TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT)
            }

            val (outType, varargElementType) =
                if (javaParameter.isVararg()) {
                    val paramType = javaParameter.getType()
                    assert (paramType is JavaArrayType, "Vararg parameter should be an array: $paramType")
                    val arrayType = c.typeResolver.transformArrayType(paramType as JavaArrayType, typeUsage, true)
                    val outType = TypeUtils.makeNotNullable(arrayType)
                    Pair(outType, KotlinBuiltIns.getInstance().getArrayElementType(outType))
                }
                else {
                    val jetType = c.typeResolver.transformJavaType(javaParameter.getType(), typeUsage)
                    if (jetType.isNullable() && javaParameter.hasNotNullAnnotation())
                        Pair(TypeUtils.makeNotNullable(jetType), null)
                    else Pair(jetType, null)
                }

            ValueParameterDescriptorImpl(
                    function,
                    index,
                    c.resolveAnnotations(javaParameter.getAnnotations()),
                    // TODO: parameter names may be drawn from attached sources, which is slow; it's better to make them lazy
                    javaParameter.getName() ?: Name.identifier("p$index"),
                    outType,
                    false,
                    varargElementType
            )
        }.toList()
    }

    override fun getFunctions(name: Name) = _functions(name)
    protected open fun getAllFunctionNames(): Collection<Name> = memberIndex().getAllMetodNames()

    // No object can be defined in Java
    override fun getObjectDescriptor(name: Name): ClassDescriptor? = null
    override fun getObjectDescriptors() = emptyList<ClassDescriptor>()

    override fun getLocalVariable(name: Name): VariableDescriptor? = null
    override fun getDeclarationsByLabel(labelName: LabelName) = emptyList<DeclarationDescriptor>()

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()
    override fun getAllDescriptors() = allDescriptors()

    private fun computeAllDescriptors(): MutableCollection<DeclarationDescriptor> {
        val result = LinkedHashSet<DeclarationDescriptor>()

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
