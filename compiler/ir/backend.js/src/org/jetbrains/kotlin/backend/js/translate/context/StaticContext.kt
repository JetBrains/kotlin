/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.js.translate.context

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.hash.LinkedHashMap
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.naming.*
import org.jetbrains.kotlin.js.naming.SuggestedName
import org.jetbrains.kotlin.js.resolve.diagnostics.JsBuiltinNameClashChecker
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.backend.js.translate.declaration.ClassModelGenerator
import org.jetbrains.kotlin.backend.js.translate.intrinsic.Intrinsics
import org.jetbrains.kotlin.backend.js.translate.utils.*
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.tasks.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.source.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.types.typeUtil.*

import java.util.*

import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.js.config.JsConfig.UNKNOWN_EXTERNAL_MODULE_NAME
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isLibraryObject
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject
import org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.backend.js.translate.utils.JsDescriptorUtils.getSuperclass

/**
 * Aggregates all the static parts of the context.
 */
class StaticContext(
    val bindingTrace: BindingTrace,
    val config: JsConfig,
    val currentModule: ModuleDescriptor,
    val sourceFilePathResolver: SourceFilePathResolver
) {
    val program = JsProgram()

    val fragment = JsProgramFragment(JsAstUtils.createFunctionWithEmptyBody(program.scope).scope)

    val namer = Namer.newInstance(program.rootScope)

    val intrinsics = Intrinsics()

    private val rootScope = fragment.scope

    private fun <T : Any> cached(body: (DeclarationDescriptor) -> T?): (DeclarationDescriptor) -> T? {
        val cache = mutableMapOf<DeclarationDescriptor, T?>()

        return { descriptor ->
            if (descriptor in cache) {
                cache[descriptor]
            } else {
                body(descriptor).also { cache[descriptor] = it }
            }
        }
    }

    private val scopes = cached { descriptor ->
        when (descriptor) {
            is PackageFragmentDescriptor -> getScopeForPackage(descriptor.fqName)
            is CallableDescriptor -> {
                val correspondingFunction = JsAstUtils.createFunctionWithEmptyBody(fragment.scope)
                assert(!scopeToFunction.containsKey(correspondingFunction.scope)) { "Scope to function value overridden for $descriptor" }
                scopeToFunction[correspondingFunction.scope] = correspondingFunction
                correspondingFunction.source = descriptor.source.getPsi()
                correspondingFunction.scope
            }
            is ClassDescriptor -> {
                val superclass = getSuperclass(descriptor)
                if (superclass != null) {
                    getScopeForDescriptor(superclass).innerObjectScope("Scope for class " + descriptor.getName())
                } else {
                    val function = JsFunction(JsRootScope(program), JsBlock(), descriptor.toString())
                    for (builtinName in BUILTIN_JS_PROPERTIES) {
                        function.scope.declareName(builtinName)
                    }
                    scopeToFunction[function.scope] = function
                    function.scope
                }
            }
            else -> fragment.scope
        }
    }

    private val innerNames = cached { descriptor ->
        if (descriptor is PackageFragmentDescriptor && DescriptorUtils.getContainingModule(descriptor) === currentModule) {
            return@cached exporter.getLocalPackageName(descriptor.fqName)
        }
        if (descriptor is FunctionDescriptor) {
            val initialDescriptor = descriptor.initialSignatureDescriptor
            if (initialDescriptor != null) {
                return@cached getInnerNameForDescriptor(initialDescriptor)
            }
        }
        if (descriptor is ModuleDescriptor) {
            return@cached getModuleInnerName(descriptor)
        }
        if (descriptor is LocalVariableDescriptor || descriptor is ParameterDescriptor) {
            return@cached getNameForDescriptor(descriptor)
        }
        if (descriptor is ConstructorDescriptor) {
            if (descriptor.isPrimary) {
                return@cached getInnerNameForDescriptor(descriptor.constructedClass)
            }
        }
        localOrImportedName(descriptor, getSuggestedName(descriptor))
    }

    private val objectInstanceNames = cached { descriptor ->
        val suggested = getSuggestedName(descriptor) + Namer.OBJECT_INSTANCE_FUNCTION_SUFFIX
        val result = JsScope.declareTemporaryName(suggested)
        val tag = generateSignature(descriptor)
        if (tag != null) {
            fragment.nameBindings.add(JsNameBinding("object:$tag", result))
        }
        result
    }

    private val scopeToFunction = hashMapOf<JsScope, JsFunction>()

    private val classOrConstructorClosure = hashMapOf<MemberDescriptor, List<DeclarationDescriptor>>()

    val deferredCallSites: MutableMap<ClassDescriptor, MutableList<DeferredCallSite>> = HashMap()

    val nameSuggestion = NameSuggestion()

    private val nameCache = HashMap<DeclarationDescriptor, JsName>()

    private val backingFieldNameCache = HashMap<VariableDescriptorWithAccessors, JsName>()

    private val fqnCache = HashMap<DeclarationDescriptor, JsExpression>()

    private val tagCache = HashMap<DeclarationDescriptor, String?>()

    private val importedModules = LinkedHashMap<JsImportedModuleKey, JsImportedModule>()

    private val exporter = DeclarationExporter(this)

    private val packageScopes = HashMap<FqName, JsScope>()

    private val classModelGenerator = ClassModelGenerator(TranslationContext.rootContext(this))

    private var nameForImportsForInline: JsName? = null

    private val modulesImportedForInline = HashMap<String, JsExpression>()

    private val specialFunctions = EnumMap<SpecialFunction, JsName>(SpecialFunction::class.java)

    private val intrinsicNames = HashMap<String, JsName>()

    private val propertyMetadataVariables = HashMap<VariableDescriptorWithAccessors, JsName>()

    private val isStdlib = run {
        val exceptionClass = currentModule.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("kotlin.Exception")))
        exceptionClass != null && DescriptorUtils.getContainingModule(exceptionClass) === currentModule
    }

    val bindingContext: BindingContext
        get() = bindingTrace.bindingContext

    val topLevelStatements: MutableList<JsStatement>
        get() = fragment.initializerBlock.statements

    val declarationStatements: MutableList<JsStatement>
        get() = fragment.declarationBlock.statements

    init {
        val kotlinName = rootScope.declareName(Namer.KOTLIN_NAME)
        createImportedModule(JsImportedModuleKey(Namer.KOTLIN_LOWER_NAME, null), Namer.KOTLIN_LOWER_NAME, kotlinName, null)
    }

    fun getScopeForDescriptor(descriptor: DeclarationDescriptor): JsScope {
        return if (descriptor is ModuleDescriptor) {
            rootScope
        } else scopes(descriptor.original) ?: error("Must have a scope for descriptor")
    }

    fun getFunctionWithScope(descriptor: CallableDescriptor): JsFunction {
        val scope = getScopeForDescriptor(descriptor)
        val function = scopeToFunction[scope]!!
        assert(scope == function.scope) { "Inconsistency." }
        return function
    }

    fun getQualifiedReference(descriptor: DeclarationDescriptor): JsNameRef {
        return getQualifiedExpression(descriptor) as JsNameRef
    }

    fun getTag(descriptor: DeclarationDescriptor): String? {
        val tag: String?
        if (!tagCache.containsKey(descriptor)) {
            tag = generateSignature(descriptor)
            tagCache[descriptor] = tag
        } else {
            tag = tagCache[descriptor]
        }
        return tag
    }

    private fun getQualifiedExpression(descriptor: DeclarationDescriptor): JsExpression {
        var fqn: JsExpression? = fqnCache[descriptor]
        if (fqn == null) {
            fqn = buildQualifiedExpression(descriptor)
            fqnCache[descriptor] = fqn
        }
        return fqn.deepCopy()
    }

    fun suggestName(descriptor: DeclarationDescriptor): SuggestedName? {
        return nameSuggestion.suggest(descriptor)
    }

    private fun buildQualifiedExpression(descriptor: DeclarationDescriptor): JsExpression {
        if (descriptor is ClassDescriptor) {
            val type = descriptor.defaultType
            if (KotlinBuiltIns.isAny(descriptor)) {
                return pureFqn("Object", null)
            } else if (KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isByte(type) ||
                KotlinBuiltIns.isFloat(type) || KotlinBuiltIns.isDouble(type)
            ) {
                return pureFqn("Number", null)
            } else if (KotlinBuiltIns.isLong(type)) {
                return pureFqn("Long", Namer.kotlinObject())
            } else if (KotlinBuiltIns.isChar(type)) {
                return pureFqn("BoxedChar", Namer.kotlinObject())
            } else if (KotlinBuiltIns.isString(type)) {
                return pureFqn("String", null)
            } else if (KotlinBuiltIns.isBoolean(type)) {
                return pureFqn("Boolean", null)
            } else if (KotlinBuiltIns.isArrayOrPrimitiveArray(descriptor)) {
                return pureFqn("Array", null)
            } else if (type.isBuiltinFunctionalType) {
                return pureFqn("Function", null)
            } else if (descriptor.defaultType.isNotNullThrowable()) {
                return pureFqn("Error", null)
            }
        }

        val suggested = suggestName(descriptor)
        if (suggested == null) {
            val module = DescriptorUtils.getContainingModule(descriptor)
            val result = getModuleExpressionFor(module)
            return result ?: pureFqn(Namer.rootPackageName, null)
        }

        if (config.moduleKind != ModuleKind.PLAIN) {
            val moduleName = AnnotationsUtils.getModuleName(suggested.descriptor)
            if (moduleName != null) {
                return JsAstUtils.pureFqn(getImportedModule(moduleName, suggested.descriptor).internalName, null)
            }
        }

        val partNames = getActualNameFromSuggested(suggested)
        var expression = when {
            isLibraryObject(suggested.descriptor) -> Namer.kotlinObject()
            isNativeObject(suggested.descriptor) && !isNativeObject(suggested.scope) ||
                    suggested.descriptor is CallableDescriptor && suggested.scope is FunctionDescriptor -> null
            else -> getQualifiedExpression(suggested.scope)
        }// Don't generate qualifier for top-level native declarations
        // Don't generate qualifier for local declarations

        if (isNativeObject(suggested.descriptor) && DescriptorUtils.isTopLevelDeclaration(suggested.descriptor)) {
            val fileModuleName = AnnotationsUtils.getFileModuleName(bindingContext, suggested.descriptor)
            if (fileModuleName != null) {
                val moduleJsName = getImportedModule(fileModuleName, null).internalName
                expression = pureFqn(moduleJsName, expression)
            }

            val qualifier = AnnotationsUtils.getFileQualifier(bindingContext, suggested.descriptor)
            if (qualifier != null) {
                for (qualifierPart in StringUtil.split(qualifier, ".")) {
                    expression = pureFqn(qualifierPart, expression)
                }
            }
        }

        for (partName in partNames) {
            expression = JsNameRef(partName, expression)
            applySideEffects(expression, suggested.descriptor)
        }
        assert(expression != null) { "Since partNames is not empty, expression must be non-null" }
        return expression!!
    }

    fun getNameForDescriptor(descriptor: DeclarationDescriptor): JsName {
        if (descriptor is ClassDescriptor && KotlinBuiltIns.isAny(descriptor)) {
            val result = rootScope.declareName("Object")
            result.descriptor = descriptor
            return result
        }
        val suggested =
            nameSuggestion.suggest(descriptor) ?: throw IllegalArgumentException("Can't generate name for root declarations: " + descriptor)
        return getActualNameFromSuggested(suggested)[0]
    }

    fun getNameForBackingField(property: VariableDescriptorWithAccessors): JsName {
        var name: JsName? = backingFieldNameCache[property]

        if (name == null) {
            val fqn = nameSuggestion.suggest(property) ?: error("Properties are non-root declarations: " + property)
            assert(fqn.names.size == 1) { "Private names must always consist of exactly one name" }

            val scope = getScopeForDescriptor(fqn.scope)
            val baseName = NameSuggestion.getPrivateMangledName(fqn.names[0], property) + "_0"
            name = scope.declareFreshName(baseName)
            backingFieldNameCache[property] = name
        }

        return name
    }

    fun getInnerNameForDescriptor(descriptor: DeclarationDescriptor): JsName {
        return innerNames(descriptor.original) ?: error("Must have inner name for descriptor")
    }

    fun getNameForObjectInstance(descriptor: ClassDescriptor): JsName {
        return objectInstanceNames(descriptor.original) ?: error("Must have inner name for object instance")
    }

    private fun getActualNameFromSuggested(suggested: SuggestedName): List<JsName> {
        var scope = getScopeForDescriptor(suggested.scope)

        if (suggested.descriptor.isDynamic()) {
            scope = JsDynamicScope
        } else if (AnnotationsUtils.isPredefinedObject(suggested.descriptor) && DescriptorUtils.isTopLevelDeclaration(suggested.descriptor)) {
            scope = rootScope
        }

        val names = ArrayList<JsName>()
        if (suggested.stable) {
            val tag = getTag(suggested.descriptor)
            var index = 0
            for (namePart in suggested.names) {
                val name = scope.declareName(namePart)
                name.descriptor = suggested.descriptor
                if (tag != null && !AnnotationsUtils.isNativeObject(suggested.descriptor) &&
                    !AnnotationsUtils.isLibraryObject(suggested.descriptor)
                ) {
                    fragment.nameBindings.add(JsNameBinding(index++.toString() + ":" + tag, name))
                }
                names.add(name)
            }
        } else {
            // TODO: consider using sealed class to represent FQNs
            assert(suggested.names.size == 1) { "Private names must always consist of exactly one name" }
            var name: JsName? = nameCache[suggested.descriptor]
            if (name == null) {
                var baseName = NameSuggestion.sanitizeName(suggested.names[0])
                if (suggested.descriptor is LocalVariableDescriptor || suggested.descriptor is ValueParameterDescriptor) {
                    name = JsScope.declareTemporaryName(baseName)
                } else {
                    if (!DescriptorUtils.isDescriptorWithLocalVisibility(suggested.descriptor)) {
                        baseName += "_0"
                    }
                    name = scope.declareFreshName(baseName)
                }
            }
            nameCache[suggested.descriptor] = name
            name.descriptor = suggested.descriptor
            val tag = getTag(suggested.descriptor)
            if (tag != null) {
                fragment.nameBindings.add(JsNameBinding(tag, name))
            }
            names.add(name)
        }

        return names
    }

    private fun importDeclaration(suggestedName: String, tag: String, declaration: JsExpression): JsName {
        val result = importDeclarationImpl(suggestedName, tag, declaration)
        fragment.nameBindings.add(JsNameBinding(tag, result))
        return result
    }

    private fun importDeclarationImpl(suggestedName: String, tag: String, declaration: JsExpression): JsName {
        val result = JsScope.declareTemporaryName(suggestedName)
        result.imported = true
        fragment.imports[tag] = declaration
        return result
    }

    private fun localOrImportedName(descriptor: DeclarationDescriptor, suggestedName: String): JsName {
        val module = descriptor.module
        val name: JsName
        var tag = getTag(descriptor)
        val isNative = AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)
        if (module !== currentModule && !isLocallyRedeclaredBuiltin(descriptor) || isNative) {
            assert(tag != null) { "Can't import declaration without tag: " + descriptor }
            val result = getQualifiedReference(descriptor)
            if (isNative && result.qualifier == null && result.name != null) {
                name = result.name!!
                tag = null
            } else {
                name = importDeclarationImpl(suggestedName, tag!!, result)
            }
        } else {
            name = JsScope.declareTemporaryName(suggestedName)
        }
        if (tag != null) {
            fragment.nameBindings.add(JsNameBinding(tag, name))
        }
        name.descriptor = descriptor
        return name
    }

    // When compiling stdlib, we may have sources for built-in declaration. In this case we have two distinct descriptors
    // for declaration with one signature. One descriptor is from current module, another descriptor is from built-in module.
    // Different declarations refer different descriptors. This may cause single name to be both imported and declared locally,
    // which in turn causes runtime error. We avoid this by detecting this case and turning off import.
    private fun isLocallyRedeclaredBuiltin(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor !is ClassDescriptor) return false
        val fqName = DescriptorUtils.getFqNameSafe(descriptor)
        val classId = ClassId.topLevel(fqName)
        val localDescriptor = currentModule.findClassAcrossModuleDependencies(classId)
        return localDescriptor != null && DescriptorUtils.getContainingModule(localDescriptor) === currentModule
    }

    private fun getScopeForPackage(fqName: FqName): JsScope {
        var scope: JsScope? = packageScopes[fqName]
        if (scope == null) {
            if (fqName.isRoot) {
                scope = JsRootScope(program)
            } else {
                val parentScope = getScopeForPackage(fqName.parent())
                scope = parentScope.innerObjectScope(fqName.shortName().asString())
            }
            packageScopes[fqName] = scope
        }
        return scope
    }

    private fun getModuleExpressionFor(descriptor: DeclarationDescriptor): JsExpression? {
        val name = getModuleInnerName(descriptor)
        return if (name != null) JsAstUtils.pureFqn(name, null) else null
    }

    private fun getModuleInnerName(descriptor: DeclarationDescriptor): JsName? {
        val module = DescriptorUtils.getContainingModule(descriptor)
        if (currentModule === module) {
            return rootScope.declareName(Namer.rootPackageName)
        }
        val moduleName = suggestModuleName(module)

        return if (UNKNOWN_EXTERNAL_MODULE_NAME == moduleName) null else getImportedModule(moduleName, null).internalName

    }

    fun getImportedModule(baseName: String, descriptor: DeclarationDescriptor?): JsImportedModule {
        val plainName = if (descriptor != null && config.moduleKind == ModuleKind.UMD) getPlainId(descriptor) else null
        val key = JsImportedModuleKey(baseName, plainName)

        var module: JsImportedModule? = importedModules[key]
        if (module == null) {
            val internalName = JsScope.declareTemporaryName(Namer.LOCAL_MODULE_PREFIX + Namer.suggestedModuleName(baseName))
            module = createImportedModule(key, baseName, internalName, if (plainName != null) pureFqn(plainName, null) else null)
        }
        return module
    }

    private fun createImportedModule(
        key: JsImportedModuleKey,
        baseName: String,
        internalName: JsName,
        plainName: JsExpression?
    ): JsImportedModule {
        val module = JsImportedModule(baseName, internalName, plainName)
        importedModules[key] = module
        fragment.importedModules.add(module)
        return module
    }

    private fun getPlainId(declaration: DeclarationDescriptor): String {
        val suggestedName = nameSuggestion.suggest(declaration)
                ?: error("Declaration should not be ModuleDescriptor, therefore suggestedName should be non-null")
        return suggestedName.names[0]
    }

    fun putClassOrConstructorClosure(localClass: MemberDescriptor, closure: List<DeclarationDescriptor>) {
        classOrConstructorClosure[localClass] = Lists.newArrayList(closure)
    }

    fun getClassOrConstructorClosure(descriptor: MemberDescriptor): List<DeclarationDescriptor>? {
        val result = classOrConstructorClosure[descriptor]
        return if (result != null) Lists.newArrayList(result) else null
    }

    fun addClass(classDescriptor: ClassDescriptor) {
        if (!AnnotationsUtils.isNativeObject(classDescriptor) && !AnnotationsUtils.isLibraryObject(classDescriptor)) {
            fragment.classes[getInnerNameForDescriptor(classDescriptor)] = classModelGenerator.generateClassModel(classDescriptor)
        }
    }

    fun export(descriptor: MemberDescriptor, force: Boolean) {
        exporter.export(descriptor, force)
    }

    fun addInlineCall(descriptor: CallableDescriptor) {
        var descriptor = JsDescriptorUtils.findRealInlineDeclaration(descriptor) as CallableDescriptor
        val tag = Namer.getFunctionTag(descriptor, config)
        var moduleExpression = exportModuleForInline(DescriptorUtils.getContainingModule(descriptor))
        if (moduleExpression == null) {
            moduleExpression = getModuleExpressionFor(descriptor)
        }
        fragment.inlineModuleMap[tag] = moduleExpression
    }

    private fun getNameForImportsForInline(): JsName {
        return nameForImportsForInline ?: run {
            val name = JsScope.declareTemporaryName(Namer.IMPORTS_FOR_INLINE_PROPERTY)
            fragment.nameBindings.add(JsNameBinding(Namer.IMPORTS_FOR_INLINE_PROPERTY, name))
            nameForImportsForInline = name
            name
        }
    }

    fun exportModuleForInline(declaration: ModuleDescriptor): JsExpression? {
        if (currentModule.builtIns.builtInsModule == declaration) return null

        val moduleName = suggestModuleName(declaration)
        return if (moduleName == Namer.KOTLIN_LOWER_NAME) null else exportModuleForInline(
            moduleName,
            getInnerNameForDescriptor(declaration)
        )

    }

    fun exportModuleForInline(moduleId: String, moduleName: JsName): JsExpression {
        var moduleRef: JsExpression? = modulesImportedForInline[moduleId]
        if (moduleRef == null) {
            val currentModuleRef = pureFqn(getInnerNameForDescriptor(currentModule), null)
            val importsRef = pureFqn(Namer.IMPORTS_FOR_INLINE_PROPERTY, currentModuleRef)
            val currentImports = pureFqn(getNameForImportsForInline(), null)

            val lhsModuleRef: JsExpression
            if (moduleId.isValidES5Identifier()) {
                moduleRef = pureFqn(moduleId, importsRef)
                lhsModuleRef = pureFqn(moduleId, currentImports)
            } else {
                moduleRef = JsArrayAccess(importsRef, JsStringLiteral(moduleId))
                moduleRef.sideEffects = SideEffectKind.PURE
                lhsModuleRef = JsArrayAccess(currentImports, JsStringLiteral(moduleId))
            }
            moduleRef.localAlias = moduleName

            val importStmt = JsExpressionStatement(JsAstUtils.assignment(lhsModuleRef, moduleName.makeRef()))
            importStmt.exportedTag = "imports:" + moduleId
            fragment.exportBlock.statements.add(importStmt)

            modulesImportedForInline[moduleId] = moduleRef
        }

        return moduleRef.deepCopy()
    }

    fun getNameForSpecialFunction(specialFunction: SpecialFunction): JsName {
        return specialFunctions.getOrPut(specialFunction) {
            val expression = Namer.createSpecialFunction(specialFunction)
            val name =
                importDeclaration(specialFunction.suggestedName, TranslationUtils.getTagForSpecialFunction(specialFunction), expression)
            name.specialFunction = specialFunction
            name
        }
    }


    fun getReferenceToIntrinsic(name: String): JsExpression {
        val resultName = intrinsicNames.getOrPut(name) {
            if (isStdlib) {
                val descriptor = findDescriptorForIntrinsic(name)
                if (descriptor != null) {
                    return@getOrPut getInnerNameForDescriptor(descriptor)
                }
            }
            importDeclaration(NameSuggestion.sanitizeName(name), "intrinsic:" + name, TranslationUtils.getIntrinsicFqn(name))
        }

        return pureFqn(resultName, null)
    }

    private fun findDescriptorForIntrinsic(name: String): DeclarationDescriptor? {
        val rootPackage = currentModule.getPackage(FqName.ROOT)
        val functionDescriptor = DescriptorUtils.getFunctionByNameOrNull(
            rootPackage.memberScope, Name.identifier(name)
        )
        return functionDescriptor ?: rootPackage.memberScope.getContributedClassifier(
            Name.identifier(name), NoLookupLocation.FROM_BACKEND
        )

    }

    fun getVariableForPropertyMetadata(property: VariableDescriptorWithAccessors): JsName {
        return propertyMetadataVariables.getOrPut(property) {
            val id = getSuggestedName(property) + "_metadata"
            val name = JsScope.declareTemporaryName(NameSuggestion.sanitizeName(id))

            // Unexpectedly! However, the only thing, for which 'imported' property is relevant, is a import clener.
            // We want similar cleanup to be performed for unused MetadataProperty instances.
            // TODO: consider a different name for 'imported' property
            name.imported = true

            val propertyNameLiteral = JsStringLiteral(property.name.asString())
            val construction = JsNew(
                getReferenceToIntrinsic("PropertyMetadata"),
                listOf(propertyNameLiteral)
            )
            fragment.declarationBlock.statements.add(JsAstUtils.newVar(name, construction))
            name
        }
    }

    companion object {

        private val BUILTIN_JS_PROPERTIES = Sets.union(
            JsBuiltinNameClashChecker.PROHIBITED_MEMBER_NAMES,
            JsBuiltinNameClashChecker.PROHIBITED_STATIC_NAMES
        )

        @JvmStatic
        fun getSuggestedName(descriptor: DeclarationDescriptor): String {
            val suggestedName = when {
                descriptor is PropertyGetterDescriptor -> "get_" + getSuggestedName(descriptor.correspondingProperty)
                descriptor is PropertySetterDescriptor -> "set_" + getSuggestedName(descriptor.correspondingProperty)
                descriptor is ConstructorDescriptor -> "init"
                descriptor.name.isSpecial -> when (descriptor) {
                    is ClassDescriptor -> if (DescriptorUtils.isAnonymousObject(descriptor)) "ObjectLiteral" else "Anonymous"
                    is FunctionDescriptor -> "lambda"
                    else -> "anonymous"
                }
                else -> NameSuggestion.sanitizeName(descriptor.name.asString())
            }

            if (descriptor !is PackageFragmentDescriptor && !DescriptorUtils.isTopLevelDeclaration(descriptor)) {
                val container = descriptor.containingDeclaration
                        ?: error("We just figured out that descriptor is not for a top-level declaration: " + descriptor)

                val separator = if (descriptor is ConstructorDescriptor) "_" else "$"

                return getSuggestedName(container) + separator + suggestedName
            }

            return suggestedName
        }

        private fun suggestModuleName(module: ModuleDescriptor): String {
            if (module === module.builtIns.builtInsModule) {
                return Namer.KOTLIN_LOWER_NAME
            } else {
                val moduleName = module.name.asString()
                return moduleName.substring(1, moduleName.length - 1)
            }
        }

        private fun applySideEffects(expression: JsExpression, descriptor: DeclarationDescriptor) {
            if (descriptor is FunctionDescriptor ||
                descriptor is PackageFragmentDescriptor ||
                descriptor is ClassDescriptor
            ) {
                expression.sideEffects = SideEffectKind.PURE
            }
        }
    }
}
