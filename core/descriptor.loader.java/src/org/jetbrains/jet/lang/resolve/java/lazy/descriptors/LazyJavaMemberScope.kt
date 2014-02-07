package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.lang.resolve.name.LabelName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
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
import org.jetbrains.jet.lang.resolve.java.lazy.hasReadOnlyAnnotation
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import java.util.LinkedHashSet
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptor
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl
import java.util.Collections
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jet.lang.resolve.java.resolver.ExternalSignatureResolver
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.utils.Printer
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaPropertyInitializerEvaluator

public abstract class LazyJavaMemberScope(
        protected val c: LazyJavaResolverContextWithTypes,
        private val _containingDeclaration: DeclarationDescriptor
) : JetScope {
    private val allDescriptors = c.storageManager.createRecursionTolerantLazyValue<Collection<DeclarationDescriptor>>(
            {computeAllDescriptors()},
            // This is to avoid the following recursive case:
            //    when computing getAllPackageNames() we ask the JavaPsiFacade for all subpackages of foo
            //    it, in turn, asks JavaElementFinder for subpackages of Kotlin package foo, which calls getAllPackageNames() recursively
            //    when on recursive call we return an empty collection, recursion collapses gracefully
            Collections.emptyList()
    )

    override fun getContainingDeclaration() = _containingDeclaration

    protected val memberIndex: NotNullLazyValue<MemberIndex> = c.storageManager.createLazyValue {
        computeMemberIndex()
    }

    protected abstract fun computeMemberIndex(): MemberIndex

    private val _functions = c.storageManager.createMemoizedFunction {
        (name: Name): Collection<FunctionDescriptor>
        ->
        val methods = memberIndex().findMethodsByName(name)
        val functions = LinkedHashSet(
                methods.iterator()
                      // values() and valueOf() are added manually, see LazyJavaClassDescriptor::getClassObjectDescriptor()
                      .filter{ m -> !DescriptorResolverUtils.shouldBeInEnumClassObject(m) }
                      .flatMap {
                              m ->
                              val function = resolveMethodToFunctionDescriptor(m, true)
                              val samAdapter = resolveSamAdapter(function)
                              if (samAdapter != null)
                                  listOf(function, samAdapter).iterator()
                              else
                                  listOf(function).iterator()
                      }.toList())

        if (_containingDeclaration is JavaPackageFragmentDescriptor) {
            val klass = c.javaClassResolver.resolveClassByFqName(_containingDeclaration.getFqName().child(name))
            if (klass is LazyJavaClassDescriptor && klass.getFunctionTypeForSamInterface() != null) {
                functions.add(SingleAbstractMethodUtils.createSamConstructorFunction(_containingDeclaration, klass))
            }
        }

        if (_containingDeclaration is ClassDescriptor) {
            val functionsFromSupertypes = getFunctionsFromSupertypes(name, _containingDeclaration);

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

    internal fun resolveMethodToFunctionDescriptor(method: JavaMethod, record: Boolean = true): SimpleFunctionDescriptor {

        val functionDescriptorImpl = JavaMethodDescriptor(_containingDeclaration, c.resolveAnnotations(method), method.getName())

        val c = c.child(functionDescriptorImpl, method.getTypeParameters().toSet())

        val methodTypeParameters = method.getTypeParameters().map { p -> c.typeParameterResolver.resolveTypeParameter(p)!! }
        val valueParameters = resolveValueParameters(c, functionDescriptorImpl, method.getValueParameters())

        val returnTypeAttrs = LazyJavaTypeAttributes(c, method, TypeUsage.MEMBER_SIGNATURE_COVARIANT) {
            if (c.hasReadOnlyAnnotation(method) && !c.hasMutableAnnotation(method))
                TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
            else
                TypeUsage.MEMBER_SIGNATURE_COVARIANT
        }

        val returnJavaType = method.getReturnType() ?: throw IllegalStateException("Constructor passed as method: $method")
        val returnType = c.typeResolver.transformJavaType(returnJavaType, returnTypeAttrs).let {
            // Annotation arguments are never null in Java
            if (method.getContainingClass().isAnnotationType()) TypeUtils.makeNotNullable(it) else it
        }

        val signatureErrors: MutableList<String>
        val superFunctions: List<FunctionDescriptor>
        val effectiveSignature: ExternalSignatureResolver.AlternativeMethodSignature
        if (_containingDeclaration is PackageFragmentDescriptor) {
            superFunctions = Collections.emptyList()
            effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(method, false, returnType, null, valueParameters, methodTypeParameters)
            signatureErrors = effectiveSignature.getErrors()
        }
        else if (_containingDeclaration is ClassDescriptor) {
            val propagated = c.externalSignatureResolver.resolvePropagatedSignature(method, _containingDeclaration, returnType, null, valueParameters, methodTypeParameters)
            superFunctions = propagated.getSuperMethods()
            effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(
                    method, !superFunctions.isEmpty(), propagated.getReturnType(),
                    propagated.getReceiverType(), propagated.getValueParameters(), propagated.getTypeParameters())

            signatureErrors = ArrayList<String>(propagated.getErrors())
            signatureErrors.addAll(effectiveSignature.getErrors())
        }
        else {
            throw IllegalStateException("Unknown class or namespace descriptor: " + _containingDeclaration)
        }

        functionDescriptorImpl.initialize(
                effectiveSignature.getReceiverType(),
                DescriptorUtils.getExpectedThisObjectIfNeeded(_containingDeclaration),
                effectiveSignature.getTypeParameters(),
                effectiveSignature.getValueParameters(),
                effectiveSignature.getReturnType(),
                Modality.convertFromFlags(method.isAbstract(), !method.isFinal()),
                method.getVisibility()
        )

        if (record) {
            c.javaResolverCache.recordMethod(method, functionDescriptorImpl)
        }

        c.methodSignatureChecker.checkSignature(method, record, functionDescriptorImpl, signatureErrors, superFunctions)

        return functionDescriptorImpl
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
                    if (c.hasMutableAnnotation(javaParameter)) TypeUsage.MEMBER_SIGNATURE_COVARIANT else TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
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
                    if (jetType.isNullable() && c.hasNotNullAnnotation(javaParameter))
                        Pair(TypeUtils.makeNotNullable(jetType), null)
                    else Pair(jetType, null)
                }

            val name = if (function.getName().asString() == "equals" &&
                           jValueParameters.size() == 1 &&
                           KotlinBuiltIns.getInstance().getNullableAnyType() == outType) {
                // This is a hack to prevent numerous warnings on Kotlin classes that inherit Java classes: if you override "equals" in such
                // class without this hack, you'll be warned that in the superclass the name is "p0" (regardless of the fact that it's
                // "other" in Any)
                // TODO: fix Java parameter name loading logic somehow (don't always load "p0", "p1", etc.)
                Name.identifier("other")
            }
            else {
                // TODO: parameter names may be drawn from attached sources, which is slow; it's better to make them lazy
                javaParameter.getName() ?: Name.identifier("p$index")
            }

            ValueParameterDescriptorImpl(
                    function,
                    index,
                    c.resolveAnnotations(javaParameter),
                    name,
                    outType,
                    false,
                    varargElementType
            )
        }.toList()
    }

    private fun resolveSamAdapter(original: SimpleFunctionDescriptor): SimpleFunctionDescriptor? {
        return if (SingleAbstractMethodUtils.isSamAdapterNecessary(original))
                    SingleAbstractMethodUtils.createSamAdapterFunction(original) as SimpleFunctionDescriptor
               else null
    }

    private fun getFunctionsFromSupertypes(name: Name, descriptor: ClassDescriptor): Set<SimpleFunctionDescriptor> {
        return descriptor.getTypeConstructor().getSupertypes().flatMap {
            it.getMemberScope().getFunctions(name).map { f -> f as SimpleFunctionDescriptor }
        }.toSet()
    }

    override fun getFunctions(name: Name) = _functions(name)
    protected open fun getAllFunctionNames(): Collection<Name> = memberIndex().getAllMethodNames()

    val _properties = c.storageManager.createMemoizedFunction {
        (name: Name) ->
        val properties = ArrayList<PropertyDescriptor>()

        val field = memberIndex().findFieldByName(name)
        if (field != null && !field.isEnumEntry()) {
            if (!DescriptorUtils.isEnumClassObject(_containingDeclaration)) {
                properties.add(resolveProperty(field))
            }
        }

        if (_containingDeclaration is ClassDescriptor) {
            val propertiesFromSupertypes = getPropertiesFromSupertypes(name, _containingDeclaration);

            properties.addAll(DescriptorResolverUtils.resolveOverrides(name, propertiesFromSupertypes, properties, _containingDeclaration,
                                               c.errorReporter));

        }

        properties
    }

    private fun resolveProperty(field: JavaField): PropertyDescriptor {
        val isVar = !field.isFinal()
        val propertyDescriptor = createPropertyDescriptor(field)
        propertyDescriptor.initialize(null, null)

        val propertyType = getPropertyType(field)
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeFieldSignature(field, propertyType, isVar)
        val signatureErrors = effectiveSignature.getErrors()
        if (!signatureErrors.isEmpty()) {
            c.externalSignatureResolver.reportSignatureErrors(propertyDescriptor, signatureErrors)
        }

        propertyDescriptor.setType(effectiveSignature.getReturnType(), Collections.emptyList(), DescriptorUtils.getExpectedThisObjectIfNeeded(getContainingDeclaration()), null : JetType?)

        if (!propertyDescriptor.isVar()) {
            propertyDescriptor.setCompileTimeInitializer(JavaPropertyInitializerEvaluator.getInstance().getInitializerConstant(field, propertyDescriptor))
        }

        c.javaResolverCache.recordField(field, propertyDescriptor);

        return propertyDescriptor
    }

    private fun createPropertyDescriptor(field: JavaField): PropertyDescriptorImpl {
        val isVar = !field.isFinal()
        val visibility = field.getVisibility()
        val annotations = c.resolveAnnotations(field)
        val propertyName = field.getName()

        return JavaPropertyDescriptor(_containingDeclaration, annotations, visibility, isVar, propertyName)
    }

    private fun getPropertyType(field: JavaField): JetType {
        // Fields do not have their own generic parameters
        val propertyType = c.typeResolver.transformJavaType(field.getType(), LazyJavaTypeAttributes(c, field, TypeUsage.MEMBER_SIGNATURE_INVARIANT))
        if (field.isFinal() && field.isStatic()) {
            return TypeUtils.makeNotNullable(propertyType)
        }
        return propertyType
    }

    private fun getPropertiesFromSupertypes(name: Name, descriptor: ClassDescriptor): Set<PropertyDescriptor> {
        return descriptor.getTypeConstructor().getSupertypes().flatMap {
            it.getMemberScope().getProperties(name).map { p -> p as PropertyDescriptor }
        }.toSet()
    }

    override fun getProperties(name: Name): Collection<VariableDescriptor> = _properties(name)
    protected open fun getAllPropertyNames(): Collection<Name> = memberIndex().getAllFieldNames()

    override fun getLocalVariable(name: Name): VariableDescriptor? = null
    override fun getDeclarationsByLabel(labelName: LabelName) = listOf<DeclarationDescriptor>()

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()
    override fun getAllDescriptors() = allDescriptors()

    private fun computeAllDescriptors(): MutableCollection<DeclarationDescriptor> {
        val result = LinkedHashSet<DeclarationDescriptor>()

        for (name in getAllClassNames()) {
            val descriptor = getClassifier(name)
            if (descriptor != null) {
                // Null signifies that a class found in Java is not present in Kotlin (e.g. package class)
                result.add(descriptor)
            }
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

    protected open fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>) {
        // Do nothing
    }

    protected abstract fun getAllClassNames(): Collection<Name>

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
