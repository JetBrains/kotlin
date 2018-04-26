/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.descriptors.replace
import org.jetbrains.kotlin.backend.common.ir.createFakeOverrideDescriptor
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.backend.common.lower.SymbolWithIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWithStarProjections
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWithoutArguments
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType

internal class TestProcessor (val context: KonanBackendContext) {

    object TEST_SUITE_CLASS: IrDeclarationOriginImpl("TEST_SUITE_CLASS")
    object TEST_SUITE_GENERATED_MEMBER: IrDeclarationOriginImpl("TEST_SUITE_GENERATED_MEMBER")

    companion object {
        val COMPANION_GETTER_NAME = Name.identifier("getCompanion")
        val INSTANCE_GETTER_NAME = Name.identifier("createInstance")

        val IGNORE_FQ_NAME = FqName.fromSegments(listOf("kotlin", "test" , "Ignore"))
    }

    val symbols = context.ir.symbols

    val baseClassSuiteDescriptor = symbols.baseClassSuite.descriptor

    private val topLevelSuiteNames = mutableSetOf<String>()

    // region Useful extensions.
    var testSuiteCnt = 0
    fun Name.synthesizeSuiteClassName() = identifier.synthesizeSuiteClassName()
    fun String.synthesizeSuiteClassName() = "$this\$test\$${testSuiteCnt++}".synthesizedName

    // IrFile always uses a forward slash as a directory separator.
    private val IrFile.fileName
        get() = name.substringAfterLast('/')
    private val IrFile.topLevelSuiteName: String
        get() {
            val packageFqName = packageFragmentDescriptor.fqName
            val shortFileName = PackagePartClassUtils.getFilePartShortName(fileName)
            return if (packageFqName.isRoot) shortFileName else "$packageFqName.$shortFileName"
        }

    private fun MutableList<TestFunction>.registerFunction(
            function: IrFunctionSymbol,
            kinds: Collection<FunctionKind>) = kinds.forEach { add(TestFunction(function, it)) }

    private fun MutableList<TestFunction>.registerFunction(function: IrFunctionSymbol, kind: FunctionKind) =
            add(TestFunction(function, kind))

    private fun <T: IrElement> IrStatementsBuilder<T>.generateFunctionRegistration(
            receiver: IrValueDeclaration,
            registerTestCase: IrFunctionSymbol,
            registerFunction: IrFunctionSymbol,
            functions: Collection<TestFunction>) {
        functions.forEach {
            if (it.kind == FunctionKind.TEST) {
                // Call registerTestCase(name: String, testFunction: () -> Unit) method.
                +irCall(registerTestCase, registerTestCase.descriptor.returnType!!.toErasedIrType()).apply {
                    dispatchReceiver = irGet(receiver)
                    putValueArgument(0, IrConstImpl.string(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.stringType,
                            it.function.descriptor.name.identifier)
                    )
                    putValueArgument(1, IrFunctionReferenceImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            descriptor.valueParameters[1].type.toErasedIrType(),
                            it.function,
                            it.function.descriptor, 0))
                    putValueArgument(2, IrConstImpl.boolean(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.booleanType,
                            it.ignored
                    ))
                }
            } else {
                // Call registerFunction(kind: TestFunctionKind, () -> Unit) method.
                +irCall(registerFunction, registerFunction.descriptor.returnType!!.toErasedIrType()).apply {
                    dispatchReceiver = irGet(receiver)
                    val testKindEntry = it.kind.runtimeKind
                    putValueArgument(0, IrGetEnumValueImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            symbols.testFunctionKind.typeWithoutArguments,
                            testKindEntry)
                    )
                    putValueArgument(1, IrFunctionReferenceImpl(UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            descriptor.valueParameters[1].type.toErasedIrType(),
                            it.function,
                            it.function.descriptor, 0))
                }
            }
        }
    }
    // endregion

    // region Classes for annotation collection.
    internal enum class FunctionKind(annotationNameString: String, runtimeKindString: String) {
        TEST("kotlin.test.Test", "") {
            override val runtimeKindName: Name  get() = throw NotImplementedError()
        },

        BEFORE_EACH("kotlin.test.BeforeEach", "BEFORE_EACH"),
        AFTER_EACH("kotlin.test.AfterEach", "AFTER_EACH"),
        BEFORE_CLASS("kotlin.test.BeforeClass", "BEFORE_CLASS"),
        AFTER_CLASS("kotlin.test.AfterClass", "AFTER_CLASS");

        val annotationFqName = FqName(annotationNameString)
        open val runtimeKindName = Name.identifier(runtimeKindString)

        companion object {
            val INSTANCE_KINDS = listOf(TEST, BEFORE_EACH, AFTER_EACH)
            val COMPANION_KINDS = listOf(BEFORE_CLASS, AFTER_CLASS)
        }
    }

    private val FunctionKind.runtimeKind: IrEnumEntrySymbol
        get() = symbols.getTestFunctionKind(this)

    private data class TestFunction(val function: IrFunctionSymbol, val kind: FunctionKind) {
        val ignored: Boolean
            get() = function.descriptor.annotations.hasAnnotation(IGNORE_FQ_NAME)
    }

    private inner class TestClass(val ownerClass: IrClassSymbol) {
        var companion: IrClassSymbol? = null
        val functions = mutableListOf<TestFunction>()

        fun registerFunction(function: IrFunctionSymbol, kinds: Collection<FunctionKind>) =
                functions.registerFunction(function, kinds)
        fun registerFunction(function: IrFunctionSymbol, kind: FunctionKind) =
                functions.registerFunction(function, kind)
    }

    private inner class AnnotationCollector(val irFile: IrFile) : IrElementVisitorVoid {
        val testClasses = mutableMapOf<IrClassSymbol, TestClass>()
        val topLevelFunctions = mutableListOf<TestFunction>()

        private fun MutableMap<IrClassSymbol, TestClass>.getTestClass(key: IrClassSymbol) =
                getOrPut(key) { TestClass(key) }

        private fun MutableMap<IrClassSymbol, TestClass>.getTestClass(key: ClassDescriptor) =
                getTestClass(symbols.symbolTable.referenceClass(key))

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        fun IrFunctionSymbol.hasAnnotation(fqName: FqName) = descriptor.annotations.any { it.fqName == fqName }

        fun registerClassFunction(classDescriptor: ClassDescriptor,
                                  function: IrFunctionSymbol,
                                  kinds: Collection<FunctionKind>) {

            fun warn(msg: String) = context.reportWarning(msg, irFile, function.owner)

            kinds.forEach { kind ->
                val annotation = kind.annotationFqName
                when (kind) {
                    in FunctionKind.INSTANCE_KINDS -> with(classDescriptor) {
                        when {
                            isInner ->
                                warn("Annotation $annotation is not allowed for methods of an inner class")
                            isAbstract() ->
                                warn("Annotation $annotation is not allowed for methods of an abstract class")
                            isCompanionObject ->
                                warn("Annotation $annotation is not allowed for methods of a companion object")
                            constructors.none { it.valueParameters.size == 0 } ->
                                warn("Test class has no default constructor: ${fqNameSafe}")
                            else ->
                                testClasses.getTestClass(classDescriptor).registerFunction(function, kind)
                        }
                    }
                    in FunctionKind.COMPANION_KINDS ->
                        when {
                            classDescriptor.isCompanionObject -> {
                                val containingClass = classDescriptor.containingDeclaration as ClassDescriptor
                                val testClass = testClasses.getTestClass(containingClass)
                                testClass.companion = symbols.symbolTable.referenceClass(classDescriptor)
                                testClass.registerFunction(function, kind)
                            }
                            classDescriptor.kind == ClassKind.OBJECT -> {
                                testClasses.getTestClass(classDescriptor).registerFunction(function, kind)
                            }
                            else -> warn("Annotation $annotation is only allowed for methods of an object " +
                                    "(named or companion) or top level functions")

                        }
                    else -> throw IllegalStateException("Unreachable")
                }
            }
        }

        fun IrFunction.checkFunctionSignature() {
            // Test runner requires test functions to have the following signature: () -> Unit.
            if (descriptor.returnType != context.builtIns.unitType) {
                context.reportCompilationError(
                        "Test function must return Unit: ${descriptor.fqNameSafe}", irFile, this
                )
            }
            if (descriptor.valueParameters.isNotEmpty()) {
                context.reportCompilationError(
                        "Test function must have no arguments: ${descriptor.fqNameSafe}", irFile, this
                )
            }
        }

        // TODO: Use symbols instead of containingDeclaration when such information is available.
        override fun visitFunction(declaration: IrFunction) {
            val symbol = declaration.symbol
            val owner = declaration.descriptor.containingDeclaration

            val kinds = FunctionKind.values().filter { symbol.hasAnnotation(it.annotationFqName)  }
            if (kinds.isEmpty()) {
                return
            }
            declaration.checkFunctionSignature()

            when (owner) {
                is PackageFragmentDescriptor -> topLevelFunctions.registerFunction(symbol, kinds)
                is ClassDescriptor -> registerClassFunction(owner, symbol, kinds)
                else -> UnsupportedOperationException("Cannot create test function $declaration (defined in $owner")
            }
        }
    }
    // endregion

    //region Symbol and IR builders

    /** Base class for getters (createInstance and getCompanion). */
    private abstract inner class GetterBuilder(val returnType: IrType,
                                               val testSuite: IrClassSymbol,
                                               val getterName: Name)
        : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrFunction>() {

        val superFunction = baseClassSuiteDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(getterName, NoLookupLocation.FROM_BACKEND)
                .single { it.valueParameters.isEmpty() }


        override fun doInitialize() {
            val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
            descriptor.initialize(
                    /* receiverParameterType        = */ null,
                    /* dispatchReceiverParameter    = */ testSuite.descriptor.thisAsReceiverParameter,
                    /* typeParameters               = */ emptyList(),
                    /* unsubstitutedValueParameters = */ emptyList(),
                    /* returnType                   = */ returnType.toKotlinType(),
                    /* modality                     = */ Modality.FINAL,
                    /* visibility                   = */ Visibilities.PROTECTED
            ).apply {
                overriddenDescriptors += superFunction
            }
        }

        override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ testSuite.descriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* name                  = */ getterName,
                        /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                        /* source                = */ SourceElement.NO_SOURCE
                )
        )
    }

    /**
     * Builds a method in `[testSuite]` class with name `[getterName]`
     * returning a reference to an object represented by `[objectSymbol]`.
     */
    private inner class ObjectGetterBuilder(val objectSymbol: IrClassSymbol, testSuite: IrClassSymbol, getterName: Name)
        : GetterBuilder(objectSymbol.typeWithoutArguments, testSuite, getterName) {

        override fun buildIr(): IrFunction = IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                TEST_SUITE_GENERATED_MEMBER,
                symbol).apply {

            this.returnType = this@ObjectGetterBuilder.returnType

            val builder = context.createIrBuilder(symbol)
            createParameterDeclarations(context.ir.symbols.symbolTable)
            body = builder.irBlockBody {
                +irReturn(IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        objectSymbol.typeWithoutArguments, objectSymbol)
                )
            }
        }
    }

    /**
     * Builds a method in `[testSuite]` class with anem `[getterName]`
     * returning a new instance of class referenced by [classSymbol].
     */
    private inner class InstanceGetterBuilder(val classSymbol: IrClassSymbol, testSuite: IrClassSymbol, getterName: Name)
        : GetterBuilder(classSymbol.typeWithStarProjections, testSuite, getterName) {

        override fun buildIr() = IrFunctionImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset =   UNDEFINED_OFFSET,
                origin =      TEST_SUITE_GENERATED_MEMBER,
                symbol =      symbol).apply {

            this.returnType = this@InstanceGetterBuilder.returnType

            val builder = context.createIrBuilder(symbol)
            createParameterDeclarations(context.ir.symbols.symbolTable)
            body = builder.irBlockBody {
                val constructor = classSymbol.owner.constructors.single { it.valueParameters.isEmpty() }
                +irReturn(irCall(constructor))
            }
        }
    }

    /**
     * Builds a constructor for a test suite class representing a test class (any class in the original IrFile with
     * method(s) annotated with @Test). The test suite class is a subclass of ClassTestSuite<T>
     * where T is the test class.
     */
    private inner class ClassSuiteConstructorBuilder(val suiteName: String,
                                                     val testClassType: KotlinType,
                                                     val testCompanionType: KotlinType,
                                                     val testSuite: IrClassSymbol,
                                                     val functions: Collection<TestFunction>,
                                                     val ignored: Boolean)
        : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

        private fun IrClassSymbol.getFunction(name: String, predicate: (FunctionDescriptor) -> Boolean) =
                symbols.symbolTable.referenceFunction(descriptor.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                        .single(predicate))

        override fun buildIr() = IrConstructorImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                TEST_SUITE_GENERATED_MEMBER,
                symbol).apply {

            returnType = testSuite.typeWithStarProjections

            createParameterDeclarations(context.ir.symbols.symbolTable)

            val registerTestCase = symbols.baseClassSuite.getFunction("registerTestCase") {
                it.valueParameters.size == 3 &&
                KotlinBuiltIns.isString(it.valueParameters[0].type) && // name: String
                it.valueParameters[1].type.isFunctionType &&           // function: testClassType.() -> Unit
                KotlinBuiltIns.isBoolean(it.valueParameters[2].type)   // ignored: Boolean
            }
            val registerFunction = symbols.baseClassSuite.getFunction("registerFunction") {
                it.valueParameters.size == 2 &&
                it.valueParameters[0].type == symbols.testFunctionKind.descriptor.defaultType && // kind: TestFunctionKind
                it.valueParameters[1].type.isFunctionType                                        // function: () -> Unit
            }

            body = context.createIrBuilder(symbol).irBlockBody {
                val superConstructor = symbols.baseClassSuiteConstructor
                +IrDelegatingConstructorCallImpl(
                        startOffset =   UNDEFINED_OFFSET,
                        endOffset =     UNDEFINED_OFFSET,
                        type =          context.irBuiltIns.unitType,
                        symbol =        symbols.symbolTable.referenceConstructor(superConstructor),
                        descriptor =    superConstructor,
                        typeArgumentsCount = 2
                ).apply {
                    putTypeArgument(0, testClassType.toErasedIrType())
                    putTypeArgument(1, testCompanionType.toErasedIrType())

                    putValueArgument(0, IrConstImpl.string(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.stringType,
                            suiteName)
                    )
                    putValueArgument(1, IrConstImpl.boolean(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.booleanType,
                            ignored
                    ))
                }
                generateFunctionRegistration(testSuite.owner.thisReceiver!!,
                        registerTestCase, registerFunction, functions)
            }
        }

        override fun doInitialize() {
            val descriptor = symbol.descriptor as ClassConstructorDescriptorImpl
            descriptor.initialize(emptyList(), Visibilities.PUBLIC).apply {
                returnType = testSuite.descriptor.defaultType
            }
        }

        override fun buildSymbol() = IrConstructorSymbolImpl(
                ClassConstructorDescriptorImpl.createSynthesized(
                        testSuite.descriptor,
                        Annotations.EMPTY,
                        true,
                        SourceElement.NO_SOURCE
                )
        )
    }

    private fun KotlinType.toErasedIrType(): IrType = context.ir.translateErased(this)

    /**
     * Builds a test suite class representing a test class (any class in the original IrFile with method(s)
     * annotated with @Test). The test suite class is a subclass of ClassTestSuite<T> where T is the test class.
     */
    private inner class ClassSuiteBuilder(testClass: IrClassSymbol,
                                          testCompanion: IrClassSymbol?,
                                          val containingDeclaration: DeclarationDescriptor,
                                          val functions: Collection<TestFunction>)
        : SymbolWithIrBuilder<IrClassSymbol, IrClass>() {

        private val IrClassSymbol.ignored: Boolean  get() = descriptor.annotations.hasAnnotation(IGNORE_FQ_NAME)
        private val IrClassSymbol.isObject: Boolean get() = descriptor.kind == ClassKind.OBJECT

        val suiteName = testClass.descriptor.fqNameSafe.toString()
        val suiteClassName = testClass.descriptor.name.synthesizeSuiteClassName()

        val testClassType = testClass.descriptor.defaultType
        val testCompanionType = if (testClass.isObject) {
            testClassType
        } else {
            testCompanion?.descriptor?.defaultType ?: context.irBuiltIns.nothing
        }

        val superType = baseClassSuiteDescriptor.defaultType.replace(listOf(testClassType, testCompanionType))

        val constructorBuilder = ClassSuiteConstructorBuilder(
            suiteName, testClassType, testCompanionType, symbol, functions, testClass.ignored
        )

        val instanceGetterBuilder: GetterBuilder
        val companionGetterBuilder: GetterBuilder?

        init {
            if (testClass.isObject) {
                instanceGetterBuilder = ObjectGetterBuilder(testClass, symbol, INSTANCE_GETTER_NAME)
                companionGetterBuilder = ObjectGetterBuilder(testClass, symbol, COMPANION_GETTER_NAME)
            } else {
                instanceGetterBuilder = InstanceGetterBuilder(testClass, symbol, INSTANCE_GETTER_NAME)
                companionGetterBuilder = testCompanion?.let {
                    ObjectGetterBuilder(it, symbol, COMPANION_GETTER_NAME)
                }
            }
        }

        override fun buildIr() = symbols.symbolTable.declareClass(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                TEST_SUITE_CLASS,
                symbol.descriptor).apply {
            createParameterDeclarations()
            addMember(constructorBuilder.ir)
            addMember(instanceGetterBuilder.ir)
            companionGetterBuilder?.let { addMember(it.ir) }
            addFakeOverrides(symbols.symbolTable)
            setSuperSymbols(symbols.symbolTable)
        }

        override fun doInitialize() {
            val descriptor = symbol.descriptor as ClassDescriptorImpl
            val constructorDescriptor = constructorBuilder.symbol.descriptor

            val contributedDescriptors = baseClassSuiteDescriptor
                    .unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map {
                        when {
                            it == instanceGetterBuilder.superFunction -> instanceGetterBuilder.symbol.descriptor
                            it == companionGetterBuilder?.superFunction -> companionGetterBuilder.symbol.descriptor
                            else -> it.createFakeOverrideDescriptor(symbol.descriptor as ClassDescriptorImpl)
                        }
                    }.filterNotNull()

            descriptor.initialize(
                    SimpleMemberScope(contributedDescriptors),setOf(constructorDescriptor), constructorDescriptor
            )

            constructorBuilder.initialize()
            instanceGetterBuilder.initialize()
            companionGetterBuilder?.initialize()
        }

        override fun buildSymbol() = symbols.symbolTable.referenceClass(
                ClassDescriptorImpl(
                        /* containingDeclaration = */ containingDeclaration,
                        /* name                  = */ suiteClassName,
                        /* modality              = */ Modality.FINAL,
                        /* kind                  = */ ClassKind.CLASS,
                        /* superTypes            = */ listOf(superType),
                        /* source                = */ SourceElement.NO_SOURCE,
                        /* isExternal            = */ false,
                        /* storageManager        = */ LockBasedStorageManager.NO_LOCKS
                )
        )
    }
    //endregion

    // region IR generation methods
    private fun generateClassSuite(irFile: IrFile, testClass: TestClass) =
            with(ClassSuiteBuilder(testClass.ownerClass,
                    testClass.companion,
                    irFile.packageFragmentDescriptor,
                    testClass.functions)) {
                initialize()
                irFile.addChild(ir)
                val irConstructor = ir.constructors.single()
                irFile.addTopLevelInitializer(
                        IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irConstructor.returnType, irConstructor.symbol)
                )
            }

    /** Check if this fqName already used or not. */
    private fun checkSuiteName(irFile: IrFile, name: String): Boolean {
        if (topLevelSuiteNames.contains(name)) {
            context.reportCompilationError("Package '${irFile.packageFragmentDescriptor.fqName}' has top-level test " +
                    "functions in several files with the same name: '${irFile.fileName}'")
            return false
        }
        topLevelSuiteNames.add(name)
        return true
    }

    private fun generateTopLevelSuite(irFile: IrFile, functions: Collection<TestFunction>) {
        val builder = context.createIrBuilder(irFile.symbol)
        val suiteName = irFile.topLevelSuiteName
        if (!checkSuiteName(irFile, suiteName)) {
            return
        }

        irFile.addTopLevelInitializer(builder.irBlock {
            val constructorCall = irCall(symbols.topLevelSuiteConstructor).apply {
                putValueArgument(0, IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                context.irBuiltIns.stringType, suiteName))
            }
            val testSuiteVal = irTemporary(constructorCall,  "topLevelTestSuite")
            generateFunctionRegistration(testSuiteVal,
                    symbols.topLevelSuiteRegisterTestCase,
                    symbols.topLevelSuiteRegisterFunction,
                    functions)
        })
    }

    private fun createTestSuites(irFile: IrFile, annotationCollector: AnnotationCollector) {
        annotationCollector.testClasses.filter {
            it.value.functions.any { it.kind == FunctionKind.TEST }
        }.forEach { _, testClass ->
            generateClassSuite(irFile, testClass)
        }
        if (annotationCollector.topLevelFunctions.isNotEmpty()) {
            generateTopLevelSuite(irFile, annotationCollector.topLevelFunctions)
        }
    }
    // endregion

    fun process(irModuleFragment: IrModuleFragment) {
        irModuleFragment.files.forEach {
            val annotationCollector = AnnotationCollector(it)
            it.acceptChildrenVoid(annotationCollector)
            createTestSuites(it, annotationCollector)
        }
    }
}