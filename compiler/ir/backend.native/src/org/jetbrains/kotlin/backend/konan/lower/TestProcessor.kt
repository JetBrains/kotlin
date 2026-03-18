/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.PreSerializationNativeSymbols
import org.jetbrains.kotlin.backend.common.ir.wrapWithLambdaCall
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.backend.konan.NativePreSerializationLoweringContext
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.SetDeclarationsParentVisitor
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.collections.plus

class TestProcessor(
    private val context: LoweringContext,
    private val sourcesModules: Set<ModuleDescriptor>? = null,
) : FileLoweringPass {
    companion object {
        val TEST_SUITE_CLASS by IrDeclarationOriginImpl.Regular
        val TEST_SUITE_GENERATED_MEMBER by IrDeclarationOriginImpl.Regular

        val COMPANION_GETTER_NAME = Name.identifier("getCompanion")
        val INSTANCE_GETTER_NAME = Name.identifier("createInstance")

        val IGNORE_FQ_NAME = FqName.fromSegments(listOf("kotlin", "test", "Ignore"))
    }

    private enum class TestProcessorFunctionKind(annotationNameString: String, val runtimeKindString: String) {
        TEST("kotlin.test.Test", ""),
        BEFORE_TEST("kotlin.test.BeforeTest", "BEFORE_TEST"),
        AFTER_TEST("kotlin.test.AfterTest", "AFTER_TEST"),
        BEFORE_CLASS("kotlin.test.BeforeClass", "BEFORE_CLASS"),
        AFTER_CLASS("kotlin.test.AfterClass", "AFTER_CLASS");

        val annotationFqName = FqName(annotationNameString)

        companion object {
            val INSTANCE_KINDS = listOf(TEST, BEFORE_TEST, AFTER_TEST)
            val COMPANION_KINDS = listOf(BEFORE_CLASS, AFTER_CLASS)
        }
    }

    private val symbols = context.symbols as PreSerializationNativeSymbols

    private val baseClassSuite = symbols.baseClassSuite.owner

    private val testFunctionKindCache by lazy {
        TestProcessorFunctionKind.entries.associateWith { kind ->
            if (kind.runtimeKindString.isEmpty())
                null
            else
                symbols.testFunctionKind.owner.declarations
                        .filterIsInstance<IrEnumEntry>()
                        .single { it.name == Name.identifier(kind.runtimeKindString) }
                        .symbol
        }
    }

    private fun getTestFunctionKind(kind: TestProcessorFunctionKind) = testFunctionKindCache[kind]!!

    private val topLevelSuiteNames = mutableSetOf<String>()

    // region Useful extensions.
    private var testSuiteCnt = 0

    private fun Name.synthesizeSuiteClassName() = identifier.synthesizeSuiteClassName()
    private fun String.synthesizeSuiteClassName() = "$this\$test\$${testSuiteCnt++}".synthesizedName

    // IrFile always uses a forward slash as a directory separator.
    private val IrFile.fileName
        get() = name.substringAfterLast('/')

    private fun MutableList<TestFunction>.registerFunction(
        function: IrFunction,
        kinds: Collection<Pair<TestProcessorFunctionKind, /* ignored: */ Boolean>>) =
        kinds.forEach { (kind, ignored) ->
            add(TestFunction(function, kind, ignored))
        }

    private fun MutableList<TestFunction>.registerFunction(
        function: IrFunction,
        kind: TestProcessorFunctionKind,
        ignored: Boolean
    ) = add(TestFunction(function, kind, ignored))

    private fun <T : IrElement> IrStatementsBuilder<T>.generateFunctionRegistration(
        receiver: IrValueDeclaration,
        registerTestCase: IrFunction,
        registerFunction: IrFunction,
        functions: Collection<TestFunction>,
        parent: IrDeclarationParent,
    ) {
        functions.forEach {
            if (it.kind == TestProcessorFunctionKind.TEST) {
                // Call registerTestCase(name: String, testFunction: () -> Unit) method.
                +irCall(registerTestCase).apply {
                    dispatchReceiver = irGet(receiver)
                    arguments[1] = irString(it.functionName)
                    arguments[2] = it.function.wrapWithLambdaCall(parent, this@TestProcessor.context)
                    arguments[3] = irBoolean(it.ignored)
                }
            } else {
                // Call registerFunction(kind: TestFunctionKind, () -> Unit) method.
                +irCall(registerFunction).apply {
                    dispatchReceiver = irGet(receiver)
                    val testKindEntry = it.kind.runtimeKind
                    arguments[1] = IrGetEnumValueImpl(
                        it.function.startOffset,
                        it.function.endOffset,
                        symbols.testFunctionKind.typeWithArguments(emptyList()),
                        testKindEntry
                    )
                    arguments[2] = it.function.wrapWithLambdaCall(parent, this@TestProcessor.context)
                }
            }
        }
    }
    // endregion

    // region Classes for annotation collection.
    private val TestProcessorFunctionKind.runtimeKind: IrEnumEntrySymbol
        get() = getTestFunctionKind(this)

    private fun IrType.isTestFunctionKind() = classifierOrNull == symbols.testFunctionKind

    private data class TestFunction(
        val function: IrFunction,
        val kind: TestProcessorFunctionKind,
        val ignored: Boolean
    ) {
        val functionName: String get() = function.name.identifier
    }

    private inner class TestClass(val ownerClass: IrClass) {
        var companion: IrClass? = null
        val functions = mutableListOf<TestFunction>()

        val suiteClassId: ClassId = ownerClass.classId ?: error(ownerClass.render())
        val suiteName: String get() = suiteClassId.asFqNameString()

        fun registerFunction(function: IrFunction, kind: TestProcessorFunctionKind, ignored: Boolean) =
                functions.registerFunction(function, kind, ignored)
    }

    private inner class AnnotationCollector(val irFile: IrFile) : IrVisitorVoid() {
        val testClasses = mutableMapOf<IrClass, TestClass>()

        val topLevelFunctions = mutableListOf<TestFunction>()
        val topLevelSuiteClassId: ClassId by lazy {
            ClassId(irFile.packageFqName, PackagePartClassUtils.getFilePartShortName(irFile.fileName).let(Name::identifier))
        }
        val topLevelSuiteName: String get() = topLevelSuiteClassId.asFqNameString()

        private fun MutableMap<IrClass, TestClass>.getTestClass(key: IrClass) =
                getOrPut(key) { TestClass(key) }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        fun IrFunctionSymbol.hasAnnotation(fqName: FqName) = owner.hasAnnotation(fqName)

        /**
         * Checks if [this] or any of its parent functions has the annotation with the given [testAnnotation].
         * If [this] contains the given annotation, returns [this].
         * If one of the parent functions contains the given annotation, returns the [IrFunctionSymbol] for it.
         * If the annotation isn't found or found only in interface methods, returns null.
         */
        fun IrFunctionSymbol.findAnnotatedFunction(testAnnotation: FqName): IrFunctionSymbol? {
            val owner = this.owner
            val parent = owner.parent
            if (parent is IrClass && parent.isInterface) {
                return null
            }

            if (hasAnnotation(testAnnotation)) {
                return this
            }

            return (owner as? IrSimpleFunction)
                ?.overriddenSymbols
                ?.firstNotNullOfOrNull {
                    it.findAnnotatedFunction(testAnnotation)
                }
        }

        fun registerClassFunction(irClass: IrClass,
                                  function: IrFunction,
                                  kinds: Collection<Pair<TestProcessorFunctionKind, /* ignored: */ Boolean>>) {

            fun warn(msg: String) = context.reportWarning(msg, irFile, function)

            kinds.forEach { (kind, ignored) ->
                val annotation = kind.annotationFqName
                when (kind) {
                    in TestProcessorFunctionKind.INSTANCE_KINDS -> with(irClass) {
                        when {
                            isInner ->
                                warn("Annotation $annotation is not allowed for methods of an inner class")

                            modality == Modality.ABSTRACT || modality == Modality.SEALED -> {
                                // We cannot create an abstract test class but it's allowed to mark its methods as
                                // tests because the class can be extended. So skip this case without warnings.
                            }

                            isCompanion ->
                                warn("Annotation $annotation is not allowed for methods of a companion object")

                            constructors.none { it.parameters.isEmpty() } ->
                                warn("Test class has no default constructor: $fqNameForIrSerialization")

                            else ->
                                testClasses.getTestClass(irClass).registerFunction(function, kind, ignored)
                        }
                    }
                    in TestProcessorFunctionKind.COMPANION_KINDS ->
                        when {
                            irClass.isCompanion -> {
                                val containingClass = irClass.parentAsClass
                                val testClass = testClasses.getTestClass(containingClass)
                                testClass.companion = irClass
                                testClass.registerFunction(function, kind, ignored)
                            }

                            irClass.kind == ClassKind.OBJECT -> {
                                testClasses.getTestClass(irClass).registerFunction(function, kind, ignored)
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
            if (!returnType.isUnit()) {
                context.reportCompilationError(
                        "Test function must return Unit: $fqNameForIrSerialization", irFile, this
                )
            }
            if (parameters.any { it.kind != IrParameterKind.DispatchReceiver }) {
                context.reportCompilationError(
                        "Test function must have no arguments: $fqNameForIrSerialization", irFile, this
                )
            }
        }

        private fun warnAboutInheritedAnnotations(
            kind: TestProcessorFunctionKind,
            function: IrFunctionSymbol,
            annotatedFunction: IrFunctionSymbol
        ) {
            if (function.owner != annotatedFunction.owner) {
                context.reportWarning(
                    "Super method has a test annotation ${kind.annotationFqName} but the overriding method doesn't. " +
                            "Note that the overriding method will still be executed.",
                    irFile,
                    function.owner
                )
            }
        }

        private fun warnAboutLoneIgnore(functionSymbol: IrFunctionSymbol): Unit = with(functionSymbol) {
            if (hasAnnotation(IGNORE_FQ_NAME) && !hasAnnotation(TestProcessorFunctionKind.TEST.annotationFqName)) {
                context.reportWarning(
                    "Unused $IGNORE_FQ_NAME annotation (not paired with ${TestProcessorFunctionKind.TEST.annotationFqName}).",
                    irFile,
                    owner
                )
            }
        }

        // TODO: Use symbols instead of containingDeclaration when such information is available.
        override fun visitFunction(declaration: IrFunction) {
            val symbol = declaration.symbol
            val parent = declaration.parent

            warnAboutLoneIgnore(symbol)
            val kinds = TestProcessorFunctionKind.entries.mapNotNull { kind ->
                symbol.findAnnotatedFunction(kind.annotationFqName)?.let { annotatedFunction ->
                    warnAboutInheritedAnnotations(kind, symbol, annotatedFunction)
                    kind to (kind == TestProcessorFunctionKind.TEST && annotatedFunction.hasAnnotation(IGNORE_FQ_NAME))
                }
            }

            if (kinds.isEmpty()) {
                return
            }
            declaration.checkFunctionSignature()

            when (parent) {
                is IrPackageFragment -> topLevelFunctions.registerFunction(declaration, kinds)
                is IrClass -> registerClassFunction(parent, declaration, kinds)
                else -> throw UnsupportedOperationException("Cannot create test function $declaration (defined in $parent")
            }
        }
    }
    // endregion

    //region Symbol and IR builders

    /**
     * Builds a method in `[owner]` class with name `[getterName]`
     * returning a reference to an object represented by `[objectSymbol]`.
     */
    private fun buildObjectGetter(objectSymbol: IrClassSymbol,
                                  owner: IrClass,
                                  getterName: Name
    ): IrSimpleFunction =
        context.irFactory.createSimpleFunction(
            owner.startOffset,
            owner.endOffset,
            TEST_SUITE_GENERATED_MEMBER,
            getterName,
            DescriptorVisibilities.PROTECTED,
            isInline = false,
            isExpect = false,
            objectSymbol.starProjectedType,
            Modality.FINAL,
            IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
        ).apply {
            parent = owner

            val superFunction = baseClassSuite.simpleFunctions()
                    .single { it.name == getterName && it.hasShape(dispatchReceiver = true) }

            parameters += createDispatchReceiverParameterWithClassParent()
            overriddenSymbols += superFunction.symbol

            body = context.createIrBuilder(symbol, symbol.owner.startOffset, symbol.owner.endOffset).irBlockBody {
                +irReturn(
                    irGetObjectValue(objectSymbol.typeWithArguments(emptyList()), objectSymbol)
                )
            }
        }

    /**
     * Builds a method in `[testSuite]` class with name `[getterName]`
     * returning a new instance of class referenced by [classSymbol].
     */
    private fun buildInstanceGetter(classSymbol: IrClassSymbol,
                                    owner: IrClass,
                                    getterName: Name
    ): IrSimpleFunction =
        context.irFactory.createSimpleFunction(
            owner.startOffset,
            owner.endOffset,
            TEST_SUITE_GENERATED_MEMBER,
            getterName,
            DescriptorVisibilities.PROTECTED,
            isInline = false,
            isExpect = false,
            classSymbol.starProjectedType,
            Modality.FINAL,
            IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
        ).apply {
            parent = owner

            val superFunction = baseClassSuite.simpleFunctions()
                    .single { it.name == getterName && it.hasShape(dispatchReceiver = true) }

            parameters += createDispatchReceiverParameterWithClassParent()
            overriddenSymbols += superFunction.symbol

            body = context.createIrBuilder(symbol, symbol.owner.startOffset, symbol.owner.endOffset).irBlockBody {
                val constructor = classSymbol.owner.constructors.single { it.parameters.isEmpty() }
                +irReturn(irCall(constructor))
            }
        }

    private val baseClassSuiteConstructor = baseClassSuite.constructors.single {
        it.hasShape(regularParameters = 2, parameterTypes = listOf(
                context.irBuiltIns.stringType,   // name: String
                context.irBuiltIns.booleanType   // ignored: Boolean
        ))
    }

    /**
     * Builds a constructor for a test suite class representing a test class (any class in the original IrFile with
     * method(s) annotated with @Test). The test suite class is a subclass of ClassTestSuite<T>
     * where T is the test class.
     */
    private fun buildClassSuiteConstructor(suiteName: String,
                                           testClassType: IrType,
                                           testCompanionType: IrType,
                                           testSuite: IrClassSymbol,
                                           owner: IrClass,
                                           functions: Collection<TestFunction>,
                                           ignored: Boolean): IrConstructor =
            context.irFactory.createConstructor(
                testSuite.owner.startOffset,
                testSuite.owner.endOffset,
                TEST_SUITE_GENERATED_MEMBER,
                Name.special("<init>"),
                DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                testSuite.starProjectedType,
                IrConstructorSymbolImpl(),
                isPrimary = true,
            ).apply {
                parent = owner

                fun IrClass.getFunction(name: String, predicate: (IrSimpleFunction) -> Boolean) =
                        simpleFunctions().single { it.name.asString() == name && predicate(it) }

                val registerTestCase = baseClassSuite.getFunction("registerTestCase") {
                    it.parameters.size == 4
                            && it.parameters[1].type.isString()    // name: String
                            && it.parameters[2].type.isFunction()  // function: testClassType.() -> Unit
                            && it.parameters[3].type.isBoolean()   // ignored: Boolean
                }
                val registerFunction = baseClassSuite.getFunction("registerFunction") {
                    it.parameters.size == 3
                            && it.parameters[1].type.isTestFunctionKind()  // kind: TestFunctionKind
                            && it.parameters[2].type.isFunction()          // function: () -> Unit
                }

                val irBuilder = context.createIrBuilder(symbol, symbol.owner.startOffset, symbol.owner.endOffset)
                body = irBuilder.irBlockBody {
                    +irDelegatingConstructorCall(baseClassSuiteConstructor).apply {
                        typeArguments[0] = testClassType
                        typeArguments[1] = testCompanionType

                        arguments[0] = irString(suiteName)
                        arguments[1] = irBoolean(ignored)
                    }
                    generateFunctionRegistration(
                            testSuite.owner.thisReceiver!!,
                            registerTestCase,
                            registerFunction,
                            functions,
                            this@apply,
                    )
                }
            }

    private val IrClass.ignored: Boolean get() = annotations.hasAnnotation(IGNORE_FQ_NAME)

    /**
     * Builds a test suite class representing a test class (any class in the original IrFile with method(s)
     * annotated with @Test). The test suite class is a subclass of ClassTestSuite<T> where T is the test class.
     */
    private fun buildClassSuite(
        suiteName: String,
        testClass: IrClass,
        testCompanion: IrClass?,
        irFile: IrFile,
        functions: Collection<TestFunction>,
    ): IrClass {
        return context.irFactory.createClass(
            testClass.startOffset,
            testClass.endOffset,
            TEST_SUITE_CLASS,
            testClass.name.synthesizeSuiteClassName(),
            DescriptorVisibilities.PRIVATE,
            IrClassSymbolImpl(),
            ClassKind.CLASS,
            Modality.FINAL,
        ).apply {
            irFile.addChild(this)
            createThisReceiverParameter()

            val testClassType = testClass.defaultType
            val testCompanionType = if (testClass.kind == ClassKind.OBJECT) {
                testClassType
            } else {
                testCompanion?.defaultType ?: context.irBuiltIns.nothingType
            }

            val constructor = buildClassSuiteConstructor(
                    suiteName, testClassType, testCompanionType, symbol, this, functions, testClass.ignored
            )

            val instanceGetter: IrFunction
            val companionGetter: IrFunction?

            if (testClass.kind == ClassKind.OBJECT) {
                instanceGetter = buildObjectGetter(testClass.symbol, this, INSTANCE_GETTER_NAME)
                companionGetter = buildObjectGetter(testClass.symbol, this, COMPANION_GETTER_NAME)
            } else {
                instanceGetter = buildInstanceGetter(testClass.symbol, this, INSTANCE_GETTER_NAME)
                companionGetter = testCompanion?.let {
                    buildObjectGetter(it.symbol, this, COMPANION_GETTER_NAME)
                }
            }

            declarations += constructor
            declarations += instanceGetter
            companionGetter?.let { declarations += it }

            superTypes += symbols.baseClassSuite.typeWith(listOf(testClassType, testCompanionType))
            addFakeOverrides((context as? NativePreSerializationLoweringContext)?.typeSystem ?: (context as CommonBackendContext).typeSystem)
        }
    }
    //endregion

    // region IR generation methods
    private fun generateClassSuite(testClass: TestClass, irFile: IrFile) =
            with(buildClassSuite(testClass.suiteName, testClass.ownerClass, testClass.companion, irFile, testClass.functions)) {
                val irConstructor = constructors.single()
                val irBuilder = context.createIrBuilder(irFile.symbol, testClass.ownerClass.startOffset, testClass.ownerClass.endOffset)
                irBuilder.irCall(irConstructor)
            }

    /** Check if this fqName already used or not. */
    private fun checkTopLevelSuiteName(irFile: IrFile, topLevelSuiteName: String): Boolean {
        if (topLevelSuiteNames.contains(topLevelSuiteName)) {
            context.reportCompilationError("Package '${irFile.packageFqName}' has top-level test " +
                    "functions in several files with the same name: '${irFile.fileName}'")
        }
        topLevelSuiteNames.add(topLevelSuiteName)
        return true
    }

    private val topLevelSuite = symbols.topLevelSuite.owner
    private val topLevelSuiteConstructor = topLevelSuite.constructors.single {
        it.hasShape(regularParameters = 1, parameterTypes = listOf(context.irBuiltIns.stringType))
    }
    private val topLevelSuiteRegisterFunction = topLevelSuite.simpleFunctions().single {
        it.name.asString() == "registerFunction"
                && it.parameters.size == 3
                && it.parameters[1].type.isTestFunctionKind()
                && it.parameters[2].type.isFunction()
    }
    private val topLevelSuiteRegisterTestCase = topLevelSuite.simpleFunctions().single {
        it.name.asString() == "registerTestCase"
                && it.parameters.size == 4
                && it.parameters[1].type.isString()
                && it.parameters[2].type.isFunction()
                && it.parameters[3].type.isBoolean()
    }

    private fun generateTopLevelSuite(irFile: IrFile, topLevelSuiteName: String, functions: Collection<TestFunction>): IrExpression? {
        val irBuilder = context.createIrBuilder(irFile.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
        if (!checkTopLevelSuiteName(irFile, topLevelSuiteName)) {
            return null
        }

        return irBuilder.irBlock {
            val constructorCall = irCall(topLevelSuiteConstructor).apply {
                arguments[0] = irString(topLevelSuiteName)
            }
            val testSuiteVal = irTemporary(constructorCall, "topLevelTestSuite")
            generateFunctionRegistration(
                    testSuiteVal,
                    topLevelSuiteRegisterTestCase,
                    topLevelSuiteRegisterFunction,
                    functions,
                    irFile,
            )
        }
    }

    private fun createTestSuites(irFile: IrFile, annotationCollector: AnnotationCollector) {
        val statements = mutableListOf<IrStatement>()

        // There is no specified order on fake override functions, so to ensure all the tests are run deterministically,
        // sort the fake override functions by name.
        for (testClass in annotationCollector.testClasses.values) {
            val functions = testClass.functions.toList()
            testClass.functions.clear()
            val fakeOverrideFunctions = mutableListOf<TestFunction>()
            for (function in functions) {
                if (function.function.isFakeOverride)
                    fakeOverrideFunctions.add(function)
                else
                    testClass.functions.add(function)
            }
            fakeOverrideFunctions.sortBy { it.functionName }
            testClass.functions.addAll(fakeOverrideFunctions)
        }

        annotationCollector.testClasses.filter {
            it.value.functions.any { it.kind == TestProcessorFunctionKind.TEST }
        }.forEach { (_, testClass) ->
            statements.add(generateClassSuite(testClass, irFile))
        }

        if (annotationCollector.topLevelFunctions.isNotEmpty()) {
            generateTopLevelSuite(irFile, annotationCollector.topLevelSuiteName, annotationCollector.topLevelFunctions)?.let { statements.add(it) }
        }

        if (statements.isNotEmpty()) {
            // This function would be replaced by a top-level field during second stage (see TestsInitializer.kt).
            // Creating fields right away won't work as it triggers a validation during IR serialization - something about
            // conflicting signatures for these fields (even though they are private). This validation can be circumvented by
            // placing the field into a property but then the second phase (TestsInitializer) would still be needed as it is
            // incorrect to mark these properties with @EagerInitialization (it is what the second pass does), as this forces
            // their deserialization which then leads to some tests from the dependencies being run which is incorrect
            // (and there are some tests on that:
            //   CompilerTestRunnerTest.testWithLibrary
            //   InfrastructureDumpedTestListingTest.testListingCompiledFromIncludedLibrary
            //   InfrastructureDumpedTestListingTest.testListingCompiledFromSources
            // ).
            context.irFactory.buildFun {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = "createTestSuites".synthesizedName
                visibility = DescriptorVisibilities.PRIVATE
                returnType = context.irBuiltIns.unitType
            }.apply {
                parent = irFile
                irFile.declarations.add(this)
                symbols.testInitializer.let { testInitializerSymbol ->
                    annotations += buildSimpleAnnotation(context.irBuiltIns, startOffset, endOffset, testInitializerSymbol.owner)
                }
                body = context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                    statements.forEach {
                        it.accept(SetDeclarationsParentVisitor, this@apply)
                        +it
                    }
                }
            }
        }

        if (annotationCollector.testClasses.isNotEmpty() || annotationCollector.topLevelFunctions.isNotEmpty()) {
            irFile.annotations += buildSimpleAnnotation(
                context.irBuiltIns, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, symbols.testsProcessed.owner
            )
        }
    }
    // endregion

    private fun shouldSkipFile(irFile: IrFile): Boolean =
            irFile.hasAnnotation(symbols.testsProcessed)
                    || irFile.moduleDescriptor.let {
                // Process test annotations in source libraries too.
                sourcesModules != null && it !in sourcesModules
            }

    override fun lower(irFile: IrFile) {
        // TODO: uses descriptors.
        if (shouldSkipFile(irFile)) return

        val annotationCollector = AnnotationCollector(irFile)
        irFile.acceptChildrenVoid(annotationCollector)
        createTestSuites(irFile, annotationCollector)
    }
}