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
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPropertyResolver
import org.jetbrains.jet.lang.resolve.java.lazy.types.toAttributes
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptor
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl
import java.util.Collections
import org.jetbrains.jet.utils.emptyOrSingletonList
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jet.utils.Printer
import org.jetbrains.jet.lang.resolve.java.resolver.ExternalSignatureResolver
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptorForObject
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl

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

    private val memberIndex = c.storageManager.createLazyValue {
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
                              val samAdapter = JavaFunctionResolver.resolveSamAdapter(function)
                              if (samAdapter != null)
                                  listOf(function, samAdapter).iterator()
                              else
                                  listOf(function).iterator()
                      }.toList())

        if (_containingDeclaration is NamespaceDescriptor) {
            val klass = JavaFunctionResolver.findClassInNamespace(_containingDeclaration, name);
            if (klass != null && klass.getFunctionTypeForSamInterface() != null) {
                functions.add(SingleAbstractMethodUtils.createSamConstructorFunction(_containingDeclaration, klass))
            }
        }

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

    internal fun resolveMethodToFunctionDescriptor(method: JavaMethod, record: Boolean = true): SimpleFunctionDescriptor {

        val functionDescriptorImpl = JavaMethodDescriptor(_containingDeclaration, c.resolveAnnotations(method.getAnnotations()), method.getName())

        val innerC = c.child(functionDescriptorImpl, method.getTypeParameters().toSet())

        val methodTypeParameters = method.getTypeParameters().map { p -> innerC.typeParameterResolver.resolveTypeParameter(p)!! }
        val valueParameters = resolveValueParameters(innerC, functionDescriptorImpl, method.getValueParameters())

        val returnTypeAttrs = LazyJavaTypeAttributes(c, method, TypeUsage.MEMBER_SIGNATURE_COVARIANT) {
            if (c.hasReadOnlyAnnotation(method) && !c.hasMutableAnnotation(method))
                TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
            else
                TypeUsage.MEMBER_SIGNATURE_COVARIANT
        }

        val returnJavaType = method.getReturnType() ?: throw IllegalStateException("Constructor passed as method: $method")
        val returnType = innerC.typeResolver.transformJavaType(returnJavaType, returnTypeAttrs)

        val signatureErrors: MutableList<String>
        val superFunctions: List<FunctionDescriptor>
        val effectiveSignature: ExternalSignatureResolver.AlternativeMethodSignature
        if (_containingDeclaration is NamespaceDescriptor) {
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
                method.getVisibility(),
                false
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
                    c.hasMutableAnnotation(javaParameter).iif(TypeUsage.MEMBER_SIGNATURE_COVARIANT, TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT)
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

    val _properties = c.storageManager.createMemoizedFunction {
        (name: Name) ->
        val properties = ArrayList<PropertyDescriptor>()

        val field = memberIndex().findFieldByName(name)
        if (field != null) {
            if (DescriptorResolverUtils.shouldBeInEnumClassObject(field) == DescriptorUtils.isEnumClassObject(_containingDeclaration)) {
                properties.add(resolveProperty(field))
            }
        }

        if (_containingDeclaration is ClassDescriptor) {
            val propertiesFromSupertypes = JavaPropertyResolver.getPropertiesFromSupertypes(name, _containingDeclaration);

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

        c.javaResolverCache.recordField(field, propertyDescriptor);

        return propertyDescriptor
    }

    private fun createPropertyDescriptor(field: JavaField): PropertyDescriptorImpl {
        val isVar = !field.isFinal()
        val visibility = field.getVisibility()
        val annotations = c.resolveAnnotations(field.getAnnotations())
        val propertyName = field.getName()

        if (field.isEnumEntry()) {
            assert(!isVar, "Enum entries should be immutable.")
            assert(DescriptorUtils.isEnumClassObject(_containingDeclaration), "Enum entries should be put into class object of enum only: " + _containingDeclaration)
            //TODO: this is a hack to indicate that this enum entry is an object
            // class descriptor for enum entries is not used by backends so for now this should be safe to use
            val dummyClassDescriptorForEnumEntryObject = ClassDescriptorImpl(_containingDeclaration, Collections.emptyList(), Modality.FINAL, propertyName)
            dummyClassDescriptorForEnumEntryObject.initialize(
                    true,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    JetScope.EMPTY,
                    Collections.emptySet(),
                    null,
                    false)
            return JavaPropertyDescriptorForObject(
                    _containingDeclaration as ClassDescriptor,
                    annotations,
                    visibility,
                    propertyName,
                    dummyClassDescriptorForEnumEntryObject
            )
        }

        return JavaPropertyDescriptor(_containingDeclaration, annotations, visibility, isVar, propertyName)
    }

    private fun getPropertyType(field: JavaField): JetType {
        // Fields do not have their own generic parameters
        val propertyType = c.typeResolver.transformJavaType(field.getType(), LazyJavaTypeAttributes(c, field, TypeUsage.MEMBER_SIGNATURE_INVARIANT))
        if (JavaPropertyResolver.isStaticFinalField(field)) {
            return TypeUtils.makeNotNullable(propertyType)
        }
        return propertyType
    }

    override fun getProperties(name: Name): Collection<VariableDescriptor> = _properties(name)
    protected open fun getAllPropertyNames(): Collection<Name> = memberIndex().getAllFieldNames()

    // No object can be defined in Java
    override fun getObjectDescriptor(name: Name): ClassDescriptor? = null
    override fun getObjectDescriptors() = emptyList<ClassDescriptor>()

    override fun getLocalVariable(name: Name): VariableDescriptor? = null
    override fun getDeclarationsByLabel(labelName: LabelName) = emptyList<DeclarationDescriptor>()

    override fun getClassifiers(name: Name) = emptyOrSingletonList(getClassifier(name))

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()
    override fun getAllDescriptors() = allDescriptors()

    private fun computeAllDescriptors(): MutableCollection<DeclarationDescriptor> {
        val result = LinkedHashSet<DeclarationDescriptor>()

        for (name in getAllPackageNames()) {
            val descriptor = getNamespace(name)
            if (descriptor != null) {
                // Null signifies that a package found in Java is not present in Kotlin
                result.add(descriptor)
            }
        }

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

    protected abstract fun getAllPackageNames(): Collection<Name>
    protected abstract fun getAllClassNames(): Collection<Name>
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
