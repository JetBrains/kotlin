/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

// MODIFIED BY GOOGLE
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClassBase
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.ir
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

// END OF MODIFICATIONS

// MODIFIED BY GOOGLE
@OptIn(ObsoleteDescriptorBasedAPI::class)
class JKlibIrLinker(
  module: ModuleDescriptor,
  val configuration: CompilerConfiguration,
  symbolTable: SymbolTable,
  val descriptorMangler: JKlibDescriptorMangler,
  val typeSystemContextFactory: (IrBuiltIns) -> IrTypeSystemContext,
  val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) : KotlinIrLinker(module, configuration, symbolTable, emptyList()) {
  lateinit var stubGenerator: DeclarationStubGenerator
  // END OF MODIFICATIONS

  override val returnUnboundSymbolsIfSignatureNotFound
    get() = false

  private val javaName = Name.identifier("java")

  private fun DeclarationDescriptor.isJavaDescriptor(): Boolean {
    if (this is PackageFragmentDescriptor) {
      return this is LazyJavaPackageFragment || fqName.startsWith(javaName)
    }
    return this is JavaClassDescriptor || this is JavaCallableMemberDescriptor || (containingDeclaration?.isJavaDescriptor() == true)
  }

  override fun platformSpecificSymbol(symbol: IrSymbol): Boolean {
    return symbol.descriptor.isJavaDescriptor()
  }

  override val irMangler: KotlinMangler.IrMangler = JKlibIrMangler()

  // MODIFIED BY GOOGLE
  override val fakeOverrideBuilder =
    IrLinkerFakeOverrideProvider(
      linker = this,
      symbolTable = symbolTable,
      mangler = irMangler,
      friendModules = emptyMap(),
      partialLinkageSupport = partialLinkageSupport,
      // Do not construct fake overrides for Java classes. These classes are created with the
      // stub generator and are already complete. Building fake overrides for them will throw
      // an IllegalStateException as class declarations symbols are already bound
      // Note: We use an origin check instead of `clazz.isFromJava()` because parents might
      // not be initialized yet, and `isFromJava()` attempts to access the parent.
      // TODO(KT-86172): Investigate the issue around property fake override and remove this filter.
      platformSpecificClassFilter =
        object : FakeOverrideClassFilter {
          override fun needToConstructFakeOverrides(clazz: IrClass): Boolean =
            clazz.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB &&
              (clazz as? IrLazyClassBase)?.isK2 != false
        },
      externalOverridabilityConditions = externalOverridabilityConditions,
    )

  override fun createTypeSystemContext(irBuiltIns: IrBuiltIns): IrTypeSystemContext =
    typeSystemContextFactory(irBuiltIns)

  // END OF MODIFICATIONS

  override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
    moduleDescriptor === moduleDescriptor.builtIns.builtInsModule ||
      moduleDescriptor.name.asString().contains("stdlib") ||
      moduleDescriptor.name.asString().contains("built-ins")

  private fun isOwnerOfClass(
    moduleDescriptor: ModuleDescriptor,
    classDescriptor: ClassDescriptor,
    isJvmModule: Boolean,
  ): Boolean {
    if (classDescriptor.isJavaDescriptor()) {
      return isJvmModule
    }
    return classDescriptor.module == moduleDescriptor ||
      classDescriptor.module == moduleDescriptor.builtIns.builtInsModule
  }

  // MODIFIED BY GOOGLE
  override fun createModuleDeserializer(
    moduleFragment: IrModuleFragment,
    klib: KotlinLibrary?,
    strategyResolver: (String) -> DeserializationStrategy,
  ): IrModuleDeserializer {
    if (klib == null) {
      return MetadataModuleDeserializer(moduleFragment, null)
    }

    val hasIr =
      try {
        (klib.ir?.irFileCount ?: 0) > 0
      } catch (e: Throwable) {
        false
      }

    if (!hasIr) {
      return MetadataModuleDeserializer(moduleFragment, klib)
    }

    val libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
    return JKlibModuleDeserializer(moduleFragment, klib, strategyResolver, libraryAbiVersion)
  }

  // END OF MODIFICATIONS

  private fun declareJavaFieldStub(symbol: IrFieldSymbol): IrField {
    return with(stubGenerator) {
      val old = stubGenerator.unboundSymbolGeneration
      try {
        stubGenerator.unboundSymbolGeneration = true
        generateFieldStub(symbol.descriptor)
      } finally {
        stubGenerator.unboundSymbolGeneration = old
      }
    }
  }

  // MODIFIED BY GOOGLE
  private fun resolveMemberDescriptor(
    classDescriptor: ClassDescriptor,
    memberName: String,
    signatureId: Long?,
  ): DeclarationDescriptor? {
    val queue = ArrayDeque<ClassDescriptor>()
    val visited = mutableSetOf<ClassDescriptor>()
    queue.add(classDescriptor)

    val propNames = mutableListOf<Name>()
    if (memberName.startsWith("get") && memberName.length > 3) {
      propNames.add(
        Name.guessByFirstCharacter(memberName.drop(3).replaceFirstChar { it.lowercase() })
      )
    }
    if (memberName.startsWith("is") && memberName.length > 2) {
      propNames.add(
        Name.guessByFirstCharacter(memberName.drop(2).replaceFirstChar { it.lowercase() })
      )
    }
    if (memberName.startsWith("<get-") && memberName.endsWith(">")) {
      propNames.add(Name.guessByFirstCharacter(memberName.removeSurrounding("<get-", ">")))
    }
    if (memberName.startsWith("<set-") && memberName.endsWith(">")) {
      propNames.add(Name.guessByFirstCharacter(memberName.removeSurrounding("<set-", ">")))
    }
    val guessedMemberName = Name.guessByFirstCharacter(memberName)
    if (!propNames.contains(guessedMemberName)) {
      propNames.add(guessedMemberName)
    }

    val hasNonZeroSignatureId = signatureId != null && signatureId != 0L
    val fallbackCandidates = mutableListOf<DeclarationDescriptor>()

    while (queue.isNotEmpty()) {
      val currentClass = queue.removeFirst()
      if (!visited.add(currentClass)) continue

      val scopes = listOf(currentClass.unsubstitutedMemberScope, currentClass.staticScope)
      val candidates = mutableListOf<DeclarationDescriptor>()

      if (memberName == "<init>") {
        candidates.addAll(currentClass.constructors)
      } else {
        for (scope in scopes) {
          candidates.addAll(
            scope.getContributedFunctions(guessedMemberName, NoLookupLocation.FROM_BACKEND)
          )
          candidates.addAll(
            scope.getContributedVariables(guessedMemberName, NoLookupLocation.FROM_BACKEND)
          )
          scope.getContributedClassifier(guessedMemberName, NoLookupLocation.FROM_BACKEND)?.let {
            candidates.add(it)
          }

          for (pName in propNames) {
            for (prop in scope.getContributedVariables(pName, NoLookupLocation.FROM_BACKEND)) {
              prop.getter?.let { candidates.add(it) }
              prop.setter?.let { candidates.add(it) }
            }
          }
        }
      }

      if (hasNonZeroSignatureId) {
        for (cand in candidates) {
          val hash = with(descriptorMangler) { cand.signatureMangle(compatibleMode = false) }
          if (hash == signatureId) {
            return cand
          }
        }
      }

      for (cand in candidates) {
        if (matchesMemberName(cand, memberName)) {
          fallbackCandidates.add(cand)
        }
      }

      for (supertype in currentClass.typeConstructor.supertypes) {
        val superClass = supertype.constructor.declarationDescriptor as? ClassDescriptor
        if (superClass != null) {
          queue.add(superClass)
        }
      }
    }

    return fallbackCandidates.firstOrNull()
  }

  private fun matchesMemberName(descriptor: DeclarationDescriptor, memberName: String): Boolean {
    if (descriptor.name.asString() == memberName) return true
    if (descriptor is PropertyGetterDescriptor) {
      val propName = descriptor.correspondingProperty.name.asString()
      return JvmAbi.getterName(propName) == memberName || "<get-$propName>" == memberName
    }
    if (descriptor is PropertySetterDescriptor) {
      val propName = descriptor.correspondingProperty.name.asString()
      return JvmAbi.setterName(propName) == memberName || "<set-$propName>" == memberName
    }
    return false
  }

  private inner class MetadataModuleDeserializer(
    moduleFragment: IrModuleFragment,
    private val _klib: KotlinLibrary?,
  ) : IrModuleDeserializer(moduleFragment, KotlinAbiVersion.CURRENT) {
    override val klib: KotlinLibrary
      get() = _klib ?: error("'klib' is not available for ${this::class.java}")

    val isJvmModule: Boolean = _klib == null
    val moduleDescriptor: ModuleDescriptor = moduleFragment.descriptor

    private val descriptorCache = mutableMapOf<IdSignature, DeclarationDescriptor?>()
    private val classCache = mutableMapOf<String, ClassDescriptor?>()
    private val memberCache = mutableMapOf<Pair<ClassDescriptor, String>, DeclarationDescriptor?>()

    private val descriptorFinder =
      DescriptorByIdSignatureFinderImpl(
        moduleDescriptor,
        descriptorMangler,
        DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY,
      )

    override fun contains(idSig: IdSignature): Boolean = resolveDescriptor(idSig) != null

    override fun getDefinedPackageNames(): Set<FqName>? {
      if (isJvmModule) return null
      return getPackagesFqNames(moduleDescriptor)
    }

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
      val result = mutableSetOf<FqName>()
      val packageFragmentProvider =
        (module as ModuleDescriptorImpl).packageFragmentProviderForModuleContentWithoutDependencies

      fun getSubPackages(fqName: FqName) {
        if (!packageFragmentProvider.isEmpty(fqName)) result += fqName
        val subPackages = packageFragmentProvider.getSubPackagesOf(fqName) { true }
        subPackages.forEach { getSubPackages(it) }
      }

      getSubPackages(FqName.ROOT)
      return result
    }

    private fun resolveDescriptor(idSig: IdSignature): DeclarationDescriptor? {
      descriptorCache[idSig]?.let {
        return it
      }
      if (descriptorCache.containsKey(idSig)) return null

      val descriptor = findDescriptorBySignature(idSig)
      descriptorCache[idSig] = descriptor
      return descriptor
    }

    private fun findDescriptorBySignature(idSig: IdSignature): DeclarationDescriptor? {
      descriptorFinder.findDescriptorBySignature(idSig)?.let {
        return it
      }

      return when (idSig) {
        is IdSignature.CommonSignature -> {
          if (idSig.nameSegments.size == 1) {
            findTopLevelDescriptor(
              idSig.packageFqName(),
              idSig.declarationFqName,
              idSig.id,
            )
          } else {
            val classFqName = idSig.declarationFqName.substringBeforeLast('.')
            val memberName = idSig.shortName
            val classDescriptor = findClassDescriptor(idSig.packageFqName, classFqName) ?: return null
            if (idSig.declarationFqName == classFqName) {
              classDescriptor
            } else {
              findMemberDescriptor(classDescriptor, memberName, idSig.id)
            }
          }
        }
        is IdSignature.CompositeSignature -> {
          findDescriptorBySignature(idSig.inner)
        }
        is IdSignature.AccessorSignature -> {
          val propDesc = findDescriptorBySignature(idSig.propertySignature) as? PropertyDescriptor
          if (propDesc != null) {
            val accessorSig = idSig.accessorSignature
            val isSetter = accessorSig.id != null && accessorSig.id != 0L &&
              (accessorSig.shortName.startsWith("set") || accessorSig.shortName.startsWith("<set-"))
            if (isSetter) propDesc.setter else propDesc.getter
          } else {
            null
          }
        }
        else -> null
      }
    }

    private fun findClassDescriptor(packageFqName: String, classFqNames: String): ClassDescriptor? {
      val key = "$packageFqName/$classFqNames"
      if (classCache.containsKey(key)) return classCache[key]

      val classId = ClassId(FqName(packageFqName), FqName(classFqNames), false)
      val descriptor = moduleDescriptor.findClassifierAcrossModuleDependencies(classId) as? ClassDescriptor
      val result = if (descriptor != null && isOwnerOfClass(moduleDescriptor, descriptor, isJvmModule)) {
        descriptor
      } else {
        null
      }
      classCache[key] = result
      return result
    }

    private fun findTopLevelDescriptor(
      packageFqName: FqName,
      declarationFqName: String,
      signatureId: Long?,
    ): DeclarationDescriptor? {
      val packageView = moduleDescriptor.getPackage(packageFqName)
      val fragments = if (isJvmModule) {
        packageView.fragments
      } else {
        packageView.fragments.filter { it.module == moduleDescriptor }
      }

      val hasNonZeroSignatureId = signatureId != null && signatureId != 0L
      val name = Name.guessByFirstCharacter(declarationFqName)
      val fallbackCandidates = mutableListOf<DeclarationDescriptor>()

      for (fragment in fragments) {
        val scope = fragment.getMemberScope()
        val candidates = mutableListOf<DeclarationDescriptor>()
        candidates.addAll(scope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND))
        candidates.addAll(scope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND))
        scope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND)?.let { candidates.add(it) }

        if (hasNonZeroSignatureId) {
          for (cand in candidates) {
            val hash = with(descriptorMangler) { cand.signatureMangle(compatibleMode = false) }
            if (hash == signatureId) return cand
          }
        }
        fallbackCandidates.addAll(candidates)
      }
      return fallbackCandidates.firstOrNull()
    }

    private fun findMemberDescriptor(
      classDescriptor: ClassDescriptor,
      memberName: String,
      signatureId: Long?,
    ): DeclarationDescriptor? {
      val key = Pair(classDescriptor, memberName)
      if (signatureId == null || signatureId == 0L) {
        if (memberCache.containsKey(key)) return memberCache[key]
      }
      val result = resolveMemberDescriptor(classDescriptor, memberName, signatureId)
      if (signatureId == null || signatureId == 0L) {
        memberCache[key] = result
      }
      return result
    }

    private fun isKotlinFakeOverride(descriptor: DeclarationDescriptor): Boolean {
      if (descriptor !is CallableMemberDescriptor) return false
      if (descriptor.isJavaDescriptor()) return false
      return descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
    }

    private fun bindDeclarationToSymbol(idSig: IdSignature, declaration: IrDeclaration) {
      val symbol = (declaration as IrSymbolOwner).symbol
      if (!symbol.isBound) {
        when (symbol) {
          is IrClassSymbol -> symbol.bind(declaration as IrClass)
          is IrSimpleFunctionSymbol -> symbol.bind(declaration as IrSimpleFunction)
          is IrPropertySymbol -> symbol.bind(declaration as IrProperty)
          is IrConstructorSymbol -> symbol.bind(declaration as IrConstructor)
          is IrEnumEntrySymbol -> symbol.bind(declaration as IrEnumEntry)
          is IrTypeAliasSymbol -> symbol.bind(declaration as IrTypeAlias)
          is IrFieldSymbol -> symbol.bind(declaration as IrField)
          else -> {}
        }
      }
    }

    override fun tryDeserializeIrSymbol(
      idSig: IdSignature,
      symbolKind: BinarySymbolData.SymbolKind,
    ): IrSymbol? {
      val descriptor = resolveDescriptor(idSig) ?: return null
      if (isKotlinFakeOverride(descriptor)) {
        return null
      }

      val declaration =
        stubGenerator.run {
          when (symbolKind) {
            BinarySymbolData.SymbolKind.CLASS_SYMBOL -> generateClassStub(descriptor as ClassDescriptor)
            BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> generatePropertyStub(descriptor as PropertyDescriptor)
            BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> generateFunctionStub(descriptor as FunctionDescriptor)
            BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> generateConstructorStub(descriptor as ClassConstructorDescriptor)
            BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> generateEnumEntryStub(descriptor as ClassDescriptor)
            BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> generateTypeAliasStub(descriptor as TypeAliasDescriptor)
            BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> generateFieldStub(descriptor as PropertyDescriptor)
            else -> error("Unexpected type $symbolKind for sig $idSig")
          }
        }

      bindDeclarationToSymbol(idSig, declaration)
      return declaration.symbol
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing =
      error("No descriptor found for $idSig")

    override fun declareIrSymbol(symbol: IrSymbol) {
      if (symbol is IrFieldSymbol) {
        declareJavaFieldStub(symbol)
      } else {
        val descriptor = symbol.descriptor
        if (isKotlinFakeOverride(descriptor)) return
        val declaration = stubGenerator.generateMemberStub(descriptor)
        if (symbol.signature != null) {
          bindDeclarationToSymbol(symbol.signature!!, declaration)
        }
      }
    }
  }

  private inner class JKlibModuleDeserializer(
    moduleFragment: IrModuleFragment,
    klib: KotlinLibrary,
    strategyResolver: (String) -> DeserializationStrategy,
    libraryAbiVersion: KotlinAbiVersion,
  ) : BasicIrModuleDeserializer(
    this,
    moduleFragment,
    klib,
    strategyResolver,
    libraryAbiVersion,
  ) {
    private val descriptorByIdSignatureFinder =
      DescriptorByIdSignatureFinderImpl(
        moduleFragment.descriptor,
        descriptorMangler,
        DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY,
      )

    private val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

    private fun isKotlinCloneable(idSig: IdSignature): Boolean {
      val signature = idSig.asPublic() ?: return false
      return (signature.packageFqName == "kotlin" && signature.firstNameSegment == "Cloneable")
    }

    override fun contains(idSig: IdSignature): Boolean =
      super.contains(idSig) ||
        (isKotlinCloneable(idSig) && descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) != null)

    override fun getDefinedPackageNames(): Set<FqName> = getPackagesFqNames(moduleFragment.descriptor)

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
      val result = mutableSetOf<FqName>()
      val packageFragmentProvider =
        (module as ModuleDescriptorImpl).packageFragmentProviderForModuleContentWithoutDependencies

      fun getSubPackages(fqName: FqName) {
        if (!packageFragmentProvider.isEmpty(fqName)) result += fqName
        val subPackages = packageFragmentProvider.getSubPackagesOf(fqName) { true }
        subPackages.forEach { getSubPackages(it) }
      }

      getSubPackages(FqName.ROOT)
      return result
    }

    override fun tryDeserializeIrSymbol(
      idSig: IdSignature,
      symbolKind: BinarySymbolData.SymbolKind,
    ): IrSymbol? {
      super.tryDeserializeIrSymbol(idSig, symbolKind)?.let {
        return it
      }
      deserializedSymbols[idSig]?.let {
        return it
      }
      val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) ?: return null
      val symbol = (stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner).symbol
      deserializedSymbols[idSig] = symbol
      return symbol
    }
  }

  override fun postProcess(irBuiltIns: IrBuiltIns, inOrAfterLinkageStep: Boolean) {
    super.postProcess(irBuiltIns, inOrAfterLinkageStep)
    if (inOrAfterLinkageStep) {
      clearFakeOverrideFields()
    }
  }

  private fun clearFakeOverrideFields() {
    val visitor =
      object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
          element.acceptChildrenVoid(this)
        }

        override fun visitProperty(declaration: IrProperty) {
          if (declaration.isFakeOverride && declaration.getter == null) {
            declaration.backingField = null
          }
          super.visitProperty(declaration)
        }
      }

    deserializersForModules.values.forEach { deserializer ->
      deserializer.moduleFragment.acceptVoid(visitor)
    }
  }
}

private fun DeclarationDescriptor.isCleanDescriptor(): Boolean {
  if (this is PropertyAccessorDescriptor) return correspondingProperty.isCleanDescriptor()
  return this is DeserializedDescriptor
}
