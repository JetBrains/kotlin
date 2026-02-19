/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.ir.createExtensionReceiver
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl.Message
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.validation.checkers.expression.IrTypeOperatorRedundancyChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.Variance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IrValidatorTest {

    private lateinit var messageCollector: MessageCollectorImpl
    private lateinit var module: IrModuleFragment
    private val defaultValidationConfig = IrValidatorConfig(checkTreeConsistency = true, checkUnboundSymbols = true)
        .withAllChecks()
        .withInlineFunctionCallsiteCheck { it.symbol.owner.name.toString() != "inlineFunctionUseSiteNotPermitted" }

    @BeforeEach
    fun setUp() {
        messageCollector = MessageCollectorImpl()

        val moduleDescriptor = ModuleDescriptorImpl(
            Name.special("<testModule>"),
            LockBasedStorageManager("IrValidatorTest"),
            DefaultBuiltIns.Instance
        )
        module = IrModuleFragmentImpl(moduleDescriptor)
    }

    private fun buildInvalidIrExpressionWithNoLocations(): IrElement {
        val stringConcatenationWithWrongType = IrStringConcatenationImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType)

        val function1 = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }.apply {
            parameters = listOf(
                createExtensionReceiver(TestIrBuiltins.stringType),
                buildValueParameter(this) {
                    name = Name.identifier("p0")
                    type = TestIrBuiltins.anyType
                    kind = IrParameterKind.Regular
                }
            )
        }
        val functionCall =
            IrCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, function1.symbol,
                typeArgumentsCount = 0,
            ).apply {
                arguments[0] = stringConcatenationWithWrongType
                arguments[1] = stringConcatenationWithWrongType
            }
        val function2 = IrFactoryImpl.buildFun {
            name = Name.identifier("bar")
            returnType = TestIrBuiltins.anyType
        }
        val body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        body.statements.add(functionCall)
        function2.body = body
        val file = createIrFile()
        file.addChild(function2)
        return file
    }

    private fun createIrFile(
        name: String = "test.kt",
        packageFqName: FqName = FqName("org.sample"),
        maxOffset: Int = 75,
    ): IrFile {
        val fileEntry = NaiveSourceBasedFileEntryImpl(name, lineStartOffsets = intArrayOf(0, 10, 25), maxOffset = maxOffset)
        return IrFileImpl(fileEntry, IrFileSymbolImpl(), packageFqName).also(module::addFile)
    }

    private fun createTrueConst() = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.booleanType, true)

    private fun buildInvalidIrTreeWithLocations(): IrElement {
        val file = createIrFile()
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }.apply {
            parameters = listOf(
                createExtensionReceiver(TestIrBuiltins.stringType),
                buildValueParameter(this) {
                    name = Name.identifier("p0")
                    type = TestIrBuiltins.anyType
                    kind = IrParameterKind.Regular
                }
            )
        }
        val body = IrFactoryImpl.createBlockBody(5, 24)
        val stringConcatenationWithWrongType = IrStringConcatenationImpl(9, 20, TestIrBuiltins.anyType)
        val functionCall =
            IrCallImpl(
                6, 23, TestIrBuiltins.anyType, function.symbol,
                typeArgumentsCount = 0,
            ).apply {
                arguments[0] = stringConcatenationWithWrongType
                arguments[1] = stringConcatenationWithWrongType
            }
        body.statements.add(functionCall)
        function.body = body
        file.addChild(function)
        return file
    }

    private fun buildValidIrTree(): IrElement {
        val file = createIrFile()
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(5, 24)
        function.body = body
        file.addChild(function)
        return file
    }

    private inline fun runValidationAndAssert(mode: IrVerificationMode, block: () -> Unit) {
        if (mode == IrVerificationMode.ERROR) {
            assertThrows<IrValidationException>(executable = block)
        } else {
            block()
        }
    }

    private fun testValidation(
        mode: IrVerificationMode,
        tree: IrElement,
        expectedMessages: List<Message>,
        config: IrValidatorConfig = defaultValidationConfig,
    ) {
        try {
            runValidationAndAssert(mode) {
                validateIr(
                    tree,
                    TestIrBuiltins,
                    config,
                    messageCollector,
                    mode,
                    phaseName = "IrValidatorTest",
                )
            }
        } finally {
            assertEquals(expectedMessages, messageCollector.messages)
        }
    }

    @Test
    fun `no validation is performed if IrVerificationMode is NONE`() {
        testValidation(
            IrVerificationMode.NONE,
            buildInvalidIrExpressionWithNoLocations(),
            emptyList(),
        )
    }

    @Test
    fun `warnings are reported and no exception is thrown if IrVerificationMode is WARNING (no debug info)`() {
        testValidation(
            IrVerificationMode.WARNING,
            buildInvalidIrExpressionWithNoLocations(),
            listOf(
                Message(
                    WARNING,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Any declared in <no parent>' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:bar visibility:public modality:FINAL <> () returnType:kotlin.Any
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `warnings are reported and no exception is thrown if IrVerificationMode is WARNING (with debug info)`() {
        testValidation(
            IrVerificationMode.WARNING,
            buildInvalidIrTreeWithLocations(),
            listOf(
                Message(
                    WARNING,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> ($receiver:kotlin.String, p0:kotlin.Any) returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 1, 10, null),
                ),
            ),
        )
    }

    @Test
    fun `sanity check for IrValidatorTest, whether IrVerificationException is really thrown if IrVerificationMode is ERROR`() {
        val exception = assertThrows<AssertionError> {
            testValidation(
                IrVerificationMode.ERROR,
                buildValidIrTree(),
                emptyList(),
            )
        }
        assertEquals(
            "Expected org.jetbrains.kotlin.ir.validation.IrValidationException to be thrown, but nothing was thrown.",
            exception.message
        )
    }

    @Test
    fun `sanity check for IrValidatorTest, whether message is really verified, when IrValidationException is really thrown if IrVerificationMode is ERROR`() {
        val exception = assertThrows<AssertionError> {
            testValidation(
                IrVerificationMode.ERROR,
                buildInvalidIrExpressionWithNoLocations(),
                listOf(
                    Message(
                        ERROR,
                        $$"""
                        IT'S THE INTENTIONALLY WRONG MESSAGE TO VERIFY THE ASSERTION ERROR WOULD HAPPEN
                        """.trimIndent(),
                        CompilerMessageLocation.create(null, 0, 0, null),
                    ),
                ),
            )
        }
        assertEquals(
            $$"""
            expected: <[error: IT'S THE INTENTIONALLY WRONG MESSAGE TO VERIFY THE ASSERTION ERROR WOULD HAPPEN]> but was: <[error: [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
            STRING_CONCATENATION type=kotlin.Any
              inside CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Any declared in <no parent>' type=kotlin.Any origin=null
                inside BLOCK_BODY
                  inside FUN name:bar visibility:public modality:FINAL <> () returnType:kotlin.Any
                    inside FILE fqName:org.sample fileName:test.kt]>
            """.trimIndent(),
            exception.message
        )
    }

    @Test
    fun `errors are reported and IrValidationError is thrown if IrVerificationMode is ERROR (no debug info)`() {
        testValidation(
            IrVerificationMode.ERROR,
            buildInvalidIrExpressionWithNoLocations(),
            listOf(
                Message(
                    ERROR,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Any declared in <no parent>' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:bar visibility:public modality:FINAL <> () returnType:kotlin.Any
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `errors are reported and IrValidationError is thrown if IrVerificationMode is ERROR (with debug info)`() {
        testValidation(
            IrVerificationMode.ERROR,
            buildInvalidIrTreeWithLocations(),
            listOf(
                Message(
                    ERROR,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> ($receiver:kotlin.String, p0:kotlin.Any) returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 1, 10, null),
                ),
            ),
        )
    }

    @Test
    fun `duplicated nodes do not stop validation if checkTreeConsistency = false`() {
        testValidation(
            IrVerificationMode.WARNING,
            buildInvalidIrTreeWithLocations(),
            listOf(
                Message(
                    WARNING,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.String, got kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> ($receiver:kotlin.String, p0:kotlin.Any) returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
                Message(
                    WARNING,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.String, got kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> ($receiver:kotlin.String, p0:kotlin.Any) returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
                Message(
                    WARNING,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Any
                    CALL 'public final fun foo ($receiver: kotlin.String, p0: kotlin.Any): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> ($receiver:kotlin.String, p0:kotlin.Any) returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 7, null),
                ),
            ),
            config = IrValidatorConfig()
                .withTypeChecks()
        )
    }

    @Test
    fun `no infinite recursion if there are cycles in IR`() {
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }
        klass.declarations.add(klass)
        val file = createIrFile()
        file.addChild(klass)
        testValidation(
            IrVerificationMode.WARNING,
            klass,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                    CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                      inside CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                )
            )
        )
    }

    @Test
    fun `incorrect parents are reported`() {
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        klass.declarations.add(function)
        function.parent = function
        val file = createIrFile()
        file.addChild(klass)
        testValidation(
            IrVerificationMode.WARNING,
            klass,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Declaration with wrong parent:
                    declaration: FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                    expectedParent: FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                    actualParent: CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                    
                    FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                        inside CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]""".trimIndent(),
                    null,
                )
            ),
        )
    }

    @Test
    fun `private functions can't be referenced from a different file`() {
        val file1 = createIrFile("a.kt")
        val function1 = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            visibility = DescriptorVisibilities.PRIVATE
        }
        file1.addChild(function1)
        val file2 = createIrFile("b.kt")
        val function2 = IrFactoryImpl.buildFun {
            name = Name.identifier("bar")
            returnType = TestIrBuiltins.unitType
        }
        val functionCall =
            IrCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.unitType, function1.symbol,
                typeArgumentsCount = 0,
            )
        val body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        body.statements.add(functionCall)
        function2.body = body
        file2.addChild(function2)
        testValidation(
            IrVerificationMode.ERROR,
            file2,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references 'private' declaration that is invisible in the current scope:
                    CALL 'private final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Unit origin=null
                      inside BLOCK_BODY
                        inside FUN name:bar visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:b.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("b.kt", 0, 0, null),
                )
            )
        )
    }

    @Test
    fun `private classes can't be referenced from a different file`() {
        val file1 = createIrFile("a.kt")
        val privateClass = IrFactoryImpl.buildClass {
            name = Name.identifier("PrivateClass")
            visibility = DescriptorVisibilities.PRIVATE
        }
        val constructor = IrFactoryImpl.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            name = SpecialNames.INIT,
            visibility = DescriptorVisibilities.PRIVATE,
            isInline = false,
            isExpect = false,
            returnType = null,
            symbol = IrConstructorSymbolImpl(),
            isPrimary = true
        )
        privateClass.addChild(constructor)
        val file2 = createIrFile("b.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val constructorCall = IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.anyType,
            symbol = constructor.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        )
        val body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        body.statements.add(constructorCall)
        function.body = body
        file1.addChild(privateClass)
        file2.addChild(function)
        testValidation(
            IrVerificationMode.ERROR,
            file2,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references 'private' declaration that is invisible in the current scope:
                    CONSTRUCTOR_CALL 'private constructor <init> () [primary] declared in org.sample.PrivateClass' type=kotlin.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:b.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("b.kt", 0, 0, null),
                )
            )
        )
    }

    @Test
    fun `private properties can't be referenced from a different file`() {
        val file1 = createIrFile("a.kt")
        val privateProperty = IrFactoryImpl.buildProperty {
            name = Name.identifier("privateProperty")
            visibility = DescriptorVisibilities.PRIVATE
        }
        val file2 = createIrFile("b.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val propertyReference = IrPropertyReferenceImplWithShape(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            symbol = privateProperty.symbol,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false,
            typeArgumentsCount = 0,
            field = null,
            getter = null,
            setter = null,
        )
        val body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        body.statements.add(propertyReference)
        function.body = body
        file1.addChild(privateProperty)
        file2.addChild(function)
        testValidation(
            IrVerificationMode.ERROR,
            file2,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references 'private' declaration that is invisible in the current scope:
                    PROPERTY_REFERENCE 'private final privateProperty [val] declared in org.sample' field=null getter=null setter=null type=kotlin.Unit origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:b.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("b.kt", 0, 0, null),
                )
            )
        )
    }

    @Test
    fun `private types can't be referenced from a different file`() {
        val file1 = createIrFile("a.kt")
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
            visibility = DescriptorVisibilities.PRIVATE
        }
        file1.addChild(klass)
        val file2 = createIrFile("b.kt")
        val subclass = IrFactoryImpl.buildClass {
            name = Name.identifier("MySubclass")
        }
        subclass.superTypes = listOf(IrSimpleTypeImpl(klass.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()))
        file2.addChild(subclass)
        testValidation(
            IrVerificationMode.ERROR,
            file2,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references 'private' declaration that is invisible in the current scope:
                    CLASS CLASS name:MySubclass modality:FINAL visibility:public superTypes:[org.sample.MyClass]
                      inside FILE fqName:org.sample fileName:b.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("b.kt", 0, 0, null),
                )
            )
        )
    }

    @Test
    fun `out-of scope usages of value parameters are reported`() {
        val file = createIrFile()
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        file.addChild(function)
        val vp = function.addValueParameter {
            name = Name.identifier("myVP")
            type = TestIrBuiltins.anyType
        }
        val field = IrFactoryImpl.buildField {
            name = Name.identifier("myField")
            type = TestIrBuiltins.anyType
            visibility = DescriptorVisibilities.PRIVATE
        }
        field.initializer = IrFactoryImpl.createExpressionBody(12, 43, IrGetValueImpl(13, 42, vp.symbol))
        file.addChild(field)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The following expression references a value that is not available in the current scope.
                    GET_VAR 'myVP: kotlin.Any declared in org.sample.foo' type=kotlin.Any origin=null
                      inside EXPRESSION_BODY
                        inside FIELD name:myField type:kotlin.Any visibility:private
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 2, 4, null),
                )
            ),
        )
    }

    // TODO: Ensure errors for public `const` fields are reported as part of resolving KT-71243.
    @Test
    fun `non-private fields are reported`() {
        val file = createIrFile()
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }
        val publicField = IrFactoryImpl.buildField {
            name = Name.identifier("publicField")
            type = TestIrBuiltins.anyType
        }
        val lateinitProperty = IrFactoryImpl.buildProperty {
            name = Name.identifier("lateinitProperty")
            isLateinit = true
        }
        val lateinitField = IrFactoryImpl.buildField {
            name = Name.identifier("lateinitField")
            type = TestIrBuiltins.anyType
        }
        lateinitProperty.backingField = lateinitField
        lateinitField.correspondingPropertySymbol = lateinitProperty.symbol
        val constProperty = IrFactoryImpl.buildProperty {
            name = Name.identifier("constProperty")
            isConst = true
        }
        val constField = IrFactoryImpl.buildField {
            name = Name.identifier("constField")
            type = TestIrBuiltins.anyType
        }
        constProperty.backingField = constField
        constField.correspondingPropertySymbol = constProperty.symbol
        klass.addChild(publicField)
        klass.addChild(lateinitProperty)
        klass.addChild(constProperty)
        file.addChild(klass)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Kotlin fields are expected to always be private
                    FIELD name:publicField type:kotlin.Any visibility:public
                      inside CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Kotlin fields are expected to always be private
                    FIELD name:lateinitField type:kotlin.Any visibility:public
                      inside PROPERTY name:lateinitProperty visibility:public modality:FINAL [lateinit,val]
                        inside CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `cross-file usages of fields are reported`() = with(IrFactoryImpl) {
        fun IrElement.fortyTwo() = IrConstImpl.int(startOffset, endOffset, TestIrBuiltins.intType, 42)

        fun IrDeclarationContainer.addField(name: String): IrField {
            return buildField {
                this.name = Name.identifier(name)
                type = TestIrBuiltins.intType
                startOffset = 1
                endOffset = 2
            }.also { field ->
                field.initializer = createExpressionBody(startOffset, endOffset, fortyTwo())
                addChild(field)
            }
        }

        val file1 = createIrFile(name = "file1.kt")
        val topLevelField = file1.addField("topLevelField")

        val memberField = buildClass {
            name = Name.identifier("MyClass")
        }.let { clazz ->
            file1.addChild(clazz)
            clazz.addField("memberField")
        }

        val file2 = createIrFile(name = "file2.kt")

        buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            startOffset = 3
            endOffset = 4
        }.apply {
            body = createBlockBody(
                startOffset,
                endOffset,
                listOf(
                    IrGetFieldImpl(startOffset, endOffset, topLevelField.symbol, topLevelField.type),
                    IrSetFieldImpl(startOffset, endOffset, topLevelField.symbol, TestIrBuiltins.unitType).apply {
                        value = fortyTwo()
                    },
                    IrGetFieldImpl(startOffset, endOffset, memberField.symbol, memberField.type),
                    IrSetFieldImpl(startOffset, endOffset, memberField.symbol, TestIrBuiltins.unitType).apply {
                        value = fortyTwo()
                    },
                ),
            )

            file2.addChild(this)
        }

        testValidation(
            IrVerificationMode.WARNING,
            file2,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Access to a field declared in another file: file1.kt
                    GET_FIELD 'FIELD name:topLevelField type:kotlin.Int visibility:public declared in org.sample' type=kotlin.Int origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:file2.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("file2.kt", 1, 4, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Access to a field declared in another file: file1.kt
                    SET_FIELD 'FIELD name:topLevelField type:kotlin.Int visibility:public declared in org.sample' type=kotlin.Unit origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:file2.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("file2.kt", 1, 4, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Access to a field declared in another file: file1.kt
                    GET_FIELD 'FIELD name:memberField type:kotlin.Int visibility:public declared in org.sample.MyClass' type=kotlin.Int origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:file2.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("file2.kt", 1, 4, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Access to a field declared in another file: file1.kt
                    SET_FIELD 'FIELD name:memberField type:kotlin.Int visibility:public declared in org.sample.MyClass' type=kotlin.Unit origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:file2.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("file2.kt", 1, 4, null),
                ),
            ),
        )
    }

    @Test
    fun `dynamic receiver of field access is reported`() = with(IrFactoryImpl) {
        fun IrElement.fortyTwo() = IrConstImpl.int(startOffset, endOffset, TestIrBuiltins.intType, 42)
        val dynamicType = IrDynamicTypeImpl(
            annotations = emptyList(),
            variance = Variance.INVARIANT
        )

        fun IrDeclarationContainer.addField(name: String): IrField {
            return buildField {
                this.name = Name.identifier(name)
                type = TestIrBuiltins.intType
                startOffset = 1
                endOffset = 2
            }.also { field ->
                field.initializer = createExpressionBody(startOffset, endOffset, fortyTwo())
                addChild(field)
            }
        }

        val file = createIrFile(name = "file.kt")
        val memberField = buildClass {
            name = Name.identifier("MyClass")
        }.let { clazz ->
            file.addChild(clazz)
            clazz.addField("memberField").also { it.visibility = DescriptorVisibilities.PRIVATE}
        }

        buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            startOffset = 3
            endOffset = 4
        }.apply {
            val vp = addValueParameter {
                name = Name.identifier("myVP")
                type = dynamicType
            }
            val receiver1 = IrGetValueImpl(startOffset, endOffset, type = dynamicType, symbol = vp.symbol)
            val receiver2 = IrGetValueImpl(startOffset, endOffset, type = dynamicType, symbol = vp.symbol)
            body = createBlockBody(
                startOffset,
                endOffset,
                listOf(
                    IrGetFieldImpl(startOffset, endOffset, memberField.symbol, memberField.type, receiver = receiver1),
                    IrSetFieldImpl(startOffset, endOffset, memberField.symbol, receiver2, fortyTwo(), TestIrBuiltins.unitType)
                ),
            )
            file.addChild(this)
        }

        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: IrFieldAccessExpression may not access fields using dynamic receiver. IrDynamicMemberExpression must be used instead.
                    GET_FIELD 'FIELD name:memberField type:kotlin.Int visibility:private declared in org.sample.MyClass' type=kotlin.Int origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> (myVP:dynamic) returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:file.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("file.kt", 1, 4, null),
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: IrFieldAccessExpression may not access fields using dynamic receiver. IrDynamicMemberExpression must be used instead.
                    SET_FIELD 'FIELD name:memberField type:kotlin.Int visibility:private declared in org.sample.MyClass' type=kotlin.Unit origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> (myVP:dynamic) returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:file.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("file.kt", 1, 4, null),
                ),
            ),
        )
    }

    @Test
    fun `out-of scope usages of type parameters are reported`() {
        val file = createIrFile()
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }
        file.addChild(klass)
        val tp = klass.addTypeParameter {
            name = Name.identifier("E")
        }
        val field = IrFactoryImpl.buildField {
            name = Name.identifier("myField")
            type = IrSimpleTypeImpl(tp.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList())
            visibility = DescriptorVisibilities.PRIVATE
        }
        file.addChild(field)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references a type parameter 'TYPE_PARAMETER name:E index:0 variance: superTypes:[] reified:false' that is not available in the current scope.
                    FIELD name:myField type:E of org.sample.MyClass visibility:private
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `innerness of classes is respected during type parameter scope validation`() {
        // class Outer<T> {
        //     inner class Inner {
        //         fun foo(): Outer<T> = this@Outer // OK
        //     }
        //     class Nested {
        //         fun foo(): Outer<T> = this@Outer // should be reported
        //     }
        // }
        val file = createIrFile()
        val outerClass = IrFactoryImpl.buildClass {
            name = Name.identifier("Outer")
        }
        val outerTP = outerClass.addTypeParameter {
            name = Name.identifier("T")
        }
        val outerType = IrSimpleTypeImpl(
            outerClass.symbol,
            SimpleTypeNullability.NOT_SPECIFIED,
            listOf(
                IrSimpleTypeImpl(outerTP.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList())
            ),
            emptyList()
        )
        outerClass.createThisReceiverParameter()
        file.addChild(outerClass)
        val innerClass = IrFactoryImpl.buildClass {
            isInner = true
            name = Name.identifier("Inner")
        }
        outerClass.addChild(innerClass)
        val nestedClass = IrFactoryImpl.buildClass {
            name = Name.identifier("Nested")
        }
        outerClass.addChild(nestedClass)

        fun createMethod(methodName: String, startOffset: Int, endOffset: Int) =
            IrFactoryImpl.buildFun {
                name = Name.identifier(methodName)
                returnType = IrSimpleTypeImpl(
                    outerClass.symbol,
                    SimpleTypeNullability.NOT_SPECIFIED,
                    listOf(
                        IrSimpleTypeImpl(outerTP.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList())
                    ),
                    emptyList()
                )
            }.apply {
                body = IrFactoryImpl.createBlockBody(
                    startOffset,
                    endOffset,
                    listOf(
                        IrReturnImpl(
                            startOffset,
                            endOffset,
                            TestIrBuiltins.nothingType,
                            symbol,
                            IrGetValueImpl(startOffset, endOffset, outerClass.thisReceiver!!.symbol)
                        )
                    ),
                )
            }

        innerClass.addChild(createMethod("innerClassMethod", 1, 11))
        nestedClass.addChild(createMethod("nestedClassMethod", 12, 56))
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references a type parameter 'TYPE_PARAMETER name:T index:0 variance: superTypes:[] reified:false' that is not available in the current scope.
                    FUN name:nestedClassMethod visibility:public modality:FINAL <> () returnType:org.sample.Outer<T of org.sample.Outer>
                      inside CLASS CLASS name:Nested modality:FINAL visibility:public superTypes:[]
                        inside CLASS CLASS name:Outer modality:FINAL visibility:public superTypes:[]
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references a type parameter 'TYPE_PARAMETER name:T index:0 variance: superTypes:[] reified:false' that is not available in the current scope.
                    GET_VAR '<this>: org.sample.Outer<T of org.sample.Outer> declared in org.sample.Outer' type=org.sample.Outer<T of org.sample.Outer> origin=null
                      inside RETURN type=kotlin.Nothing from='public final fun nestedClassMethod (): org.sample.Outer<T of org.sample.Outer> declared in org.sample.Outer.Nested'
                        inside BLOCK_BODY
                          inside FUN name:nestedClassMethod visibility:public modality:FINAL <> () returnType:org.sample.Outer<T of org.sample.Outer>
                            inside CLASS CLASS name:Nested modality:FINAL visibility:public superTypes:[]
                              inside CLASS CLASS name:Outer modality:FINAL visibility:public superTypes:[]
                                inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 2, 3, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The following expression references a value that is not available in the current scope.
                    GET_VAR '<this>: org.sample.Outer<T of org.sample.Outer> declared in org.sample.Outer' type=org.sample.Outer<T of org.sample.Outer> origin=null
                      inside RETURN type=kotlin.Nothing from='public final fun nestedClassMethod (): org.sample.Outer<T of org.sample.Outer> declared in org.sample.Outer.Nested'
                        inside BLOCK_BODY
                          inside FUN name:nestedClassMethod visibility:public modality:FINAL <> () returnType:org.sample.Outer<T of org.sample.Outer>
                            inside CLASS CLASS name:Nested modality:FINAL visibility:public superTypes:[]
                              inside CLASS CLASS name:Outer modality:FINAL visibility:public superTypes:[]
                                inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 2, 3, null),
                ),
            )
        )
    }

    @Test
    fun `not validated, if vararg param of type BooleanArray does not have varargElementType=Boolean`() {
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        val param = function.addValueParameter(Name.identifier("v"), TestIrBuiltins.booleanArrayType)
        param.varargElementType = TestIrBuiltins.anyType
        val file = createIrFile()
        file.addChild(function)

        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Vararg type=kotlin.BooleanArray is expected to be an array of its underlying varargElementType=kotlin.Any
                    VALUE_PARAMETER kind:Regular name:v index:0 type:kotlin.BooleanArray varargElementType:kotlin.Any [vararg]
                      inside FUN name:foo visibility:public modality:FINAL <> (v:kotlin.BooleanArray) returnType:kotlin.Any
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                )
            )
        )
    }

    @Test
    fun `not validated, if vararg param of type Array of String does not have varargElementType=String`() {
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        val param = function.addValueParameter(Name.identifier("v"), TestIrBuiltins.arrayOfStringType)
        param.varargElementType = TestIrBuiltins.anyType
        val file = createIrFile()
        file.addChild(function)

        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Vararg type=kotlin.Array<kotlin.String> is expected to be an array of its underlying varargElementType=kotlin.Any
                    VALUE_PARAMETER kind:Regular name:v index:0 type:kotlin.Array<kotlin.String> varargElementType:kotlin.Any [vararg]
                      inside FUN name:foo visibility:public modality:FINAL <> (v:kotlin.Array<kotlin.String>) returnType:kotlin.Any
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                )
            )
        )
    }

    @Test
    fun `not validated, if passed vararg of type BooleanArray does not have varargElementType=Boolean`() {
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        val param = function.addValueParameter(Name.identifier("v"), TestIrBuiltins.booleanArrayType)
        param.varargElementType = TestIrBuiltins.booleanType

        val body = IrFactoryImpl.createBlockBody(5, 24)
        val vararg = IrVarargImpl(9, 20, TestIrBuiltins.booleanArrayType, TestIrBuiltins.anyType)
        val functionCall =
            IrCallImpl(
                6, 23, TestIrBuiltins.anyType, function.symbol,
                typeArgumentsCount = 0,
            ).apply {
                arguments[0] = vararg
            }
        body.statements.add(functionCall)
        function.body = body

        val file = createIrFile()
        file.addChild(function)

        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Vararg type=kotlin.BooleanArray is expected to be an array of its underlying varargElementType=kotlin.Any
                    VARARG type=kotlin.BooleanArray varargElementType=kotlin.Any
                      inside CALL 'public final fun foo (vararg v: kotlin.Boolean): kotlin.Any declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> (v:kotlin.BooleanArray) returnType:kotlin.Any
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null)
                )
            )
        )
    }

    @Test
    fun `not validated, if passed vararg of type Array of String does not have varargElementType=String`() {
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        val param = function.addValueParameter(Name.identifier("v"), TestIrBuiltins.arrayOfStringType)
        param.varargElementType = TestIrBuiltins.stringType

        val body = IrFactoryImpl.createBlockBody(5, 24)
        val vararg = IrVarargImpl(9, 20, TestIrBuiltins.arrayOfStringType, TestIrBuiltins.anyType)
        val functionCall =
            IrCallImpl(
                6, 23, TestIrBuiltins.anyType, function.symbol,
                typeArgumentsCount = 0,
            ).apply {
                arguments[0] = vararg
            }
        body.statements.add(functionCall)
        function.body = body

        val file = createIrFile()
        file.addChild(function)

        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Vararg type=kotlin.Array<kotlin.String> is expected to be an array of its underlying varargElementType=kotlin.Any
                    VARARG type=kotlin.Array<kotlin.String> varargElementType=kotlin.Any
                      inside CALL 'public final fun foo (vararg v: kotlin.String): kotlin.Any declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> (v:kotlin.Array<kotlin.String>) returnType:kotlin.Any
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null)
                )
            )
        )
    }

    @Test
    fun `accesses to not permitted inline function use site are reported`() {
        val function1 = IrFactoryImpl.buildFun {
            name = Name.identifier("inlineFunctionUseSiteNotPermitted")
            returnType = TestIrBuiltins.anyType
            isInline = true
        }
        val function2 = IrFactoryImpl.buildFun {
            name = Name.identifier("inlineFunctionUseSitePermitted")
            returnType = TestIrBuiltins.anyType
            isInline = true
        }
        val function3 = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        val functionCall1 = IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, function1.symbol,
            typeArgumentsCount = 0,
        )
        val functionCall2 = IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, function2.symbol,
            typeArgumentsCount = 0,
        )
        val body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        body.statements.add(functionCall1)
        body.statements.add(functionCall2)
        function3.body = body
        val file = createIrFile()
        file.addChild(function1)
        file.addChild(function2)
        file.addChild(function3)
        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: The following element references public inline function inlineFunctionUseSiteNotPermitted
                    CALL 'public final fun inlineFunctionUseSiteNotPermitted (): kotlin.Any [inline] declared in org.sample' type=kotlin.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Any
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                )
            )
        )
    }

    @Test
    fun `dispatch receivers with dynamic type are reported`() {
        val file = createIrFile()
        val dynamicType: IrDynamicType = IrDynamicTypeImpl(
            annotations = emptyList(),
            variance = Variance.INVARIANT
        )
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        function.addValueParameter {
            name = SpecialNames.THIS
            type = dynamicType
        }.also {
            it.kind = IrParameterKind.DispatchReceiver
        }

        val functionReference = IrFunctionReferenceImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.anyType,
            symbol = function.symbol,
            typeArgumentsCount = 0
        )

        val functionCall = IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.unitType, function.symbol,
            typeArgumentsCount = 0,
        )

        val body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        body.statements.add(functionReference)
        body.statements.add(functionCall)
        function.body = body
        file.addChild(function)

        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Dispatch receivers with 'dynamic' type are not allowed
                    FUN name:foo visibility:public modality:FINAL <> (<this>:dynamic) returnType:kotlin.Unit
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
            ),
        )
    }

    @OptIn(DelicateIrParameterIndexSetter::class, DeprecatedForRemovalCompilerApi::class)
    @Test
    fun `functions with incorrect parameter index are reported`() {
        val file = createIrFile()
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        function.addValueParameter {
            name = Name.identifier("x")
            type = TestIrBuiltins.anyType
        }
        function.addTypeParameter {
            name = Name.identifier("T")
        }
        function.parameters[0].indexInOldValueParameters = 1
        function.parameters[0].indexInParameters = 1
        function.typeParameters[0].index = 1
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Inconsistent index (old API) of value parameter 1 != 0
                    FUN name:foo visibility:public modality:FINAL <T> (x:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Inconsistent index (new API) of value parameter 1 != 0
                    FUN name:foo visibility:public modality:FINAL <T> (x:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Inconsistent index of type parameter 1 != 0
                    FUN name:foo visibility:public modality:FINAL <T> (x:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
            )
        )
    }

    @Test
    fun `functions with incorrect parameters order are reported`() {
        val file = createIrFile()
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        repeat(4) { i ->
            function.addValueParameter {
                name = Name.identifier("x$i")
                type = TestIrBuiltins.anyType
            }
        }
        function.parameters[0].kind = IrParameterKind.Regular
        function.parameters[1].kind = IrParameterKind.ExtensionReceiver
        function.parameters[2].kind = IrParameterKind.Context
        function.parameters[3].kind = IrParameterKind.DispatchReceiver
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Invalid order of function parameters: ExtensionReceiver is placed after Regular.
                    Parameters must follow a strict order: [dispatch receiver, context parameters, extension receiver, regular parameters].
                    FUN name:foo visibility:public modality:FINAL <> (x0:kotlin.Any, x1:kotlin.Any, x2:kotlin.Any, x3:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Invalid order of function parameters: Context is placed after ExtensionReceiver.
                    Parameters must follow a strict order: [dispatch receiver, context parameters, extension receiver, regular parameters].
                    FUN name:foo visibility:public modality:FINAL <> (x0:kotlin.Any, x1:kotlin.Any, x2:kotlin.Any, x3:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Invalid order of function parameters: DispatchReceiver is placed after Context.
                    Parameters must follow a strict order: [dispatch receiver, context parameters, extension receiver, regular parameters].
                    FUN name:foo visibility:public modality:FINAL <> (x0:kotlin.Any, x1:kotlin.Any, x2:kotlin.Any, x3:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
            )
        )
    }

    @Test
    fun `functions with multiple receiver parameters are reported`() {
        val file = createIrFile()
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        repeat(4) { i ->
            function.addValueParameter {
                name = Name.identifier("x$i")
                type = TestIrBuiltins.anyType
            }
        }
        function.parameters[0].kind = IrParameterKind.DispatchReceiver
        function.parameters[1].kind = IrParameterKind.DispatchReceiver
        function.parameters[2].kind = IrParameterKind.ExtensionReceiver
        function.parameters[3].kind = IrParameterKind.ExtensionReceiver
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: Function may have only one DispatchReceiver parameter
                    FUN name:foo visibility:public modality:FINAL <> (x0:kotlin.Any, x1:kotlin.Any, x2:kotlin.Any, x3:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
                Message(
                    WARNING,
                    $$"""
                    [IR VALIDATION] IrValidatorTest: Function may have only one ExtensionReceiver parameter
                    FUN name:foo visibility:public modality:FINAL <> (x0:kotlin.Any, x1:kotlin.Any, x2:kotlin.Any, x3:kotlin.Any) returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                )
            )
        )
    }

    @Test
    fun `Orphaned property getter or setter are reported`() {
        val file = createIrFile()
        val property = IrFactoryImpl.buildProperty {
            name = Name.identifier("p")
        }

        val correctPropertyGetter = IrFactoryImpl.buildFun {
            name = Name.identifier("bar")
            returnType = TestIrBuiltins.anyType
        }
        correctPropertyGetter.correspondingPropertySymbol = property.symbol
        property.getter = correctPropertyGetter

        val orphanedPropertyFunction = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        orphanedPropertyFunction.correspondingPropertySymbol = property.symbol

        val orphanedPropertyFunctionCall = IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, orphanedPropertyFunction.symbol,
            typeArgumentsCount = 0,
        )

        val orphanedPropertyFunctionReference = IrFunctionReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, orphanedPropertyFunction.symbol,
            typeArgumentsCount = 0
        )

        val body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        body.statements.add(orphanedPropertyFunctionCall)
        body.statements.add(orphanedPropertyFunctionReference)
        orphanedPropertyFunction.body = body

        file.addChild(property)
        file.addChild(orphanedPropertyFunction)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Orphaned property getter/setter FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Any
                    FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Any
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                )
            )
        )
    }

    @Test
    fun `getter and setter with inconsistent property symbols are reported`() {
        val file = createIrFile()

        val property = IrFactoryImpl.buildProperty {
            name = Name.identifier("p")
        }.apply {
            getter = IrFactoryImpl.buildFun {
                name = Name.identifier("foo")
                returnType = TestIrBuiltins.anyType
            }
            setter = IrFactoryImpl.buildFun {
                name = Name.identifier("bar")
                returnType = TestIrBuiltins.anyType
            }
        }

        file.addChild(property)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Getter of property 'PROPERTY name:p visibility:public modality:FINAL [val]' has an inconsistent corresponding property symbol.
                    PROPERTY name:p visibility:public modality:FINAL [val]
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Setter of property 'PROPERTY name:p visibility:public modality:FINAL [val]' has an inconsistent corresponding property symbol.
                    PROPERTY name:p visibility:public modality:FINAL [val]
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null)
                )
            )
        )
    }

    @Test
    fun `assignments to value parameters not marked assignable are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val nonAssignableValueParameter = function.addValueParameter {
            name = Name.identifier("p1")
            type = TestIrBuiltins.anyType
            isAssignable = false
        }
        val incorrectSetValueExpression = IrSetValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            symbol = IrValueParameterSymbolImpl(),
            value = IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.intType, 42),
            origin = null
        )

        val assignableValueParameter = function.addValueParameter {
            name = Name.identifier("p2")
            type = TestIrBuiltins.anyType
            isAssignable = true
        }
        val correctSetValueExpression = IrSetValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            symbol = IrValueParameterSymbolImpl(),
            value = IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.intType, 42),
            origin = null
        )

        incorrectSetValueExpression.symbol = nonAssignableValueParameter.symbol
        correctSetValueExpression.symbol = assignableValueParameter.symbol

        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            statements = listOf(incorrectSetValueExpression, correctSetValueExpression)
        )
        function.body = body
        file.addChild(function)

        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Assignment to value parameters not marked assignable
                    SET_VAR 'p1: kotlin.Any declared in org.sample.foo' type=kotlin.Unit origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> (p1:kotlin.Any, p2:kotlin.Any) returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `loops, breaks and continues with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            statements = listOf()
        )

        val incorrectLoop = IrWhileLoopImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, null).apply {
            condition = createTrueConst()
        }
        val correctLoop = IrWhileLoopImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.unitType, null).apply {
            condition = createTrueConst()
        }
        val incorrectBreak = IrBreakImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, incorrectLoop)
        val incorrectContinue = IrContinueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, incorrectLoop)
        val correctBreak = IrBreakImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.nothingType, incorrectLoop)
        val correctContinue = IrContinueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.nothingType, incorrectLoop)

        incorrectLoop.body = IrBlockImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            origin = null,
            statements = listOf(incorrectBreak, incorrectContinue)
        )
        correctLoop.body = IrBlockImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            origin = null,
            statements = listOf(correctBreak, correctContinue)
        )

        body.statements.addAll(listOf(incorrectLoop, correctLoop))
        function.body = body
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Nothing, got kotlin.Any
                    BREAK label=null loop.label=null
                      inside BLOCK type=kotlin.Unit origin=null
                        inside WHILE label=null origin=null
                          inside BLOCK_BODY
                            inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Nothing, got kotlin.Any
                    CONTINUE label=null loop.label=null
                      inside BLOCK type=kotlin.Unit origin=null
                        inside WHILE label=null origin=null
                          inside BLOCK_BODY
                            inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Any
                    WHILE label=null origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `getters and setters for values and fields with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )
        val field = IrFactoryImpl.buildField {
            name = Name.identifier("field")
            type = TestIrBuiltins.intType
            visibility = DescriptorVisibilities.PRIVATE
        }
        val variable = IrVariableImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            symbol = IrVariableSymbolImpl(),
            name = Name.identifier("b"),
            type = TestIrBuiltins.booleanType,
            isVar = true,
            isConst = false,
            isLateinit = true,
        ).apply {
            parent = function
        }

        val incorrectGetField = IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, TestIrBuiltins.booleanType)
        val incorrectSetField = IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, TestIrBuiltins.booleanType).apply {
            value = createTrueConst()
        }
        val correctGetField = IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, TestIrBuiltins.intType)
        val correctSetField = IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, TestIrBuiltins.unitType).apply {
            value = createTrueConst()
        }
        val incorrectSetValue = IrSetValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.anyType,
            symbol = variable.symbol,
            value = createTrueConst(),
            origin = null,
        )
        val incorrectGetValue = IrGetValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.intType,
            symbol = variable.symbol,
        )
        val correctSetValue = IrSetValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            symbol = variable.symbol,
            value = createTrueConst(),
            origin = null,
        )
        val correctGetValue = IrGetValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType,
            symbol = variable.symbol,
        )

        body.statements.addAll(
            listOf(
                variable,
                incorrectGetField,
                incorrectSetField,
                correctGetField,
                correctSetField,
                incorrectSetValue,
                incorrectGetValue,
                correctSetValue,
                correctGetValue
            )
        )
        function.body = body
        file.addChild(field)
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Int, got kotlin.Boolean
                    GET_FIELD 'FIELD name:field type:kotlin.Int visibility:private declared in org.sample' type=kotlin.Boolean origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Boolean
                    SET_FIELD 'FIELD name:field type:kotlin.Int visibility:private declared in org.sample' type=kotlin.Boolean origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Any
                    SET_VAR 'var b: kotlin.Boolean [lateinit,var] declared in org.sample.foo' type=kotlin.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Boolean, got kotlin.Int
                    GET_VAR 'var b: kotlin.Boolean [lateinit,var] declared in org.sample.foo' type=kotlin.Int origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `overrides of private declarations are reported`() {
        val file = createIrFile("test.kt")
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }
        val subclass = IrFactoryImpl.buildClass {
            name = Name.identifier("MySubclass")
        }.apply {
            superTypes = listOf(IrSimpleTypeImpl(klass.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()))
        }

        val privateFunction = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            parent = klass
        }
        klass.declarations.add(privateFunction)

        val privateFunctionOverride = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }.apply {
            parent = subclass
            overriddenSymbols = listOf(privateFunction.symbol)
        }
        subclass.declarations.add(privateFunctionOverride)

        val publicFunction = IrFactoryImpl.buildFun {
            name = Name.identifier("bar")
            returnType = TestIrBuiltins.unitType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = klass
        }
        klass.declarations.add(publicFunction)

        val publicFunctionOverride = IrFactoryImpl.buildFun {
            name = Name.identifier("bar")
            returnType = TestIrBuiltins.unitType
        }.apply {
            parent = subclass
            overriddenSymbols = listOf(publicFunction.symbol)
        }
        subclass.declarations.add(publicFunctionOverride)

        val privateProperty = IrFactoryImpl.buildProperty {
            name = Name.identifier("p1")
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            parent = klass
        }
        klass.declarations.add(privateProperty)

        val privatePropertyOverride = IrFactoryImpl.buildProperty {
            name = Name.identifier("p1")
        }.apply {
            parent = subclass
            overriddenSymbols = listOf(privateProperty.symbol)
        }
        subclass.declarations.add(privatePropertyOverride)

        val publicProperty = IrFactoryImpl.buildProperty {
            name = Name.identifier("p2")
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = klass
        }
        klass.declarations.add(publicProperty)

        val publicPropertyOverride = IrFactoryImpl.buildProperty {
            name = Name.identifier("p2")
        }.apply {
            parent = subclass
            overriddenSymbols = listOf(publicProperty.symbol)
        }
        subclass.declarations.add(publicPropertyOverride)

        file.addChild(klass)
        file.addChild(subclass)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Overrides private declaration FUN name:foo visibility:private modality:FINAL <> () returnType:kotlin.Unit
                    FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside CLASS CLASS name:MySubclass modality:FINAL visibility:public superTypes:[org.sample.MyClass]
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Overrides private declaration PROPERTY name:p1 visibility:private modality:FINAL [val]
                    PROPERTY name:p1 visibility:public modality:FINAL [val]
                      inside CLASS CLASS name:MySubclass modality:FINAL visibility:public superTypes:[org.sample.MyClass]
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `implicit coercions to unit with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val incorrectCoercion = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.intType,
            operator = IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            typeOperand = TestIrBuiltins.intType,
            argument = IrConstImpl.int(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = TestIrBuiltins.intType,
                value = 42
            )
        )

        val correctCoercion = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            operator = IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            typeOperand = TestIrBuiltins.unitType,
            argument = IrConstImpl.int(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = TestIrBuiltins.intType,
                value = 42
            )
        )

        body.statements.addAll(listOf(incorrectCoercion, correctCoercion))
        function.body = body
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: typeOperand is kotlin.Int
                    TYPE_OP type=kotlin.Int origin=IMPLICIT_COERCION_TO_UNIT typeOperand=kotlin.Int
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `null constants with non-nullable type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val nullConstWithNonNullableType = IrConstImpl.constNull(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.intType
        )

        val nullConstWithNullableType = IrConstImpl.constNull(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.intType.makeNullable()
        )

        body.statements.addAll(listOf(nullConstWithNonNullableType, nullConstWithNullableType))
        function.body = body
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: expected a nullable type, got kotlin.Int
                    CONST Null type=kotlin.Int value=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `GetObjectValue with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val myObject = IrFactoryImpl.buildClass {
            name = Name.identifier("MyObject")
            kind = ClassKind.OBJECT
        }

        val incorrectGetObjectValue = IrGetObjectValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            symbol = myObject.symbol,
            type = TestIrBuiltins.intType
        )

        val correctGetObjectValue = IrGetObjectValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            symbol = myObject.symbol,
            type = myObject.symbol.createType(false, listOf())
        )

        body.statements.addAll(listOf(incorrectGetObjectValue, correctGetObjectValue))
        function.body = body
        file.addChild(myObject)
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected org.sample.MyObject, got kotlin.Int
                    GET_OBJECT 'CLASS OBJECT name:MyObject modality:FINAL visibility:public superTypes:[]' type=kotlin.Int
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `type operator calls with incorrect types are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val incorrectCast = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType.makeNullable(),
            operator = IrTypeOperator.CAST,
            typeOperand = TestIrBuiltins.booleanType,
            argument = createTrueConst()
        )

        val correctCast = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType,
            operator = IrTypeOperator.CAST,
            typeOperand = TestIrBuiltins.booleanType,
            argument = createTrueConst()
        )

        val incorrectSafeCast = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType,
            operator = IrTypeOperator.SAFE_CAST,
            typeOperand = TestIrBuiltins.booleanType,
            argument = createTrueConst()
        )

        val correctSafeCast = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType.makeNullable(),
            operator = IrTypeOperator.SAFE_CAST,
            typeOperand = TestIrBuiltins.booleanType,
            argument = createTrueConst()
        )

        val incorrectInstanceOf = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.intType,
            operator = IrTypeOperator.INSTANCEOF,
            typeOperand = TestIrBuiltins.intType,
            argument = createTrueConst()
        )

        val correctInstanceOf = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType,
            operator = IrTypeOperator.INSTANCEOF,
            typeOperand = TestIrBuiltins.intType,
            argument = createTrueConst()
        )

        body.statements.addAll(
            listOf(
                incorrectCast,
                correctCast,
                incorrectSafeCast,
                correctSafeCast,
                incorrectInstanceOf,
                correctInstanceOf
            )
        )
        function.body = body
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Boolean, got kotlin.Boolean?
                    TYPE_OP type=kotlin.Boolean? origin=CAST typeOperand=kotlin.Boolean
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Boolean?, got kotlin.Boolean
                    TYPE_OP type=kotlin.Boolean origin=SAFE_CAST typeOperand=kotlin.Boolean
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Boolean, got kotlin.Int
                    TYPE_OP type=kotlin.Int origin=INSTANCEOF typeOperand=kotlin.Int
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `redundant IMPLICIT_CAST is reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        // Redundant: result type equals argument type
        val redundantImplicitCast = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType,
            operator = IrTypeOperator.IMPLICIT_CAST,
            typeOperand = TestIrBuiltins.booleanType,
            argument = createTrueConst()
        )

        // Non-redundant: argument type differs from result type
        val nonRedundantImplicitCast = IrTypeOperatorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.anyType,
            operator = IrTypeOperator.IMPLICIT_CAST,
            typeOperand = TestIrBuiltins.anyType,
            argument = createTrueConst() // Boolean is a subtype of Any
        )

        body.statements.addAll(listOf(redundantImplicitCast, nonRedundantImplicitCast))
        function.body = body
        file.addChild(function)

        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Redundant IMPLICIT_CAST: TYPE_OP type=kotlin.Boolean origin=IMPLICIT_CAST typeOperand=kotlin.Boolean
                    TYPE_OP type=kotlin.Boolean origin=IMPLICIT_CAST typeOperand=kotlin.Boolean
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
            config = defaultValidationConfig.withCheckers(IrTypeOperatorRedundancyChecker)
        )
    }

    @Test
    fun `calls with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val incorrectCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, function.symbol)

        val correctCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.unitType, function.symbol)

        body.statements.addAll(listOf(incorrectCall, correctCall))
        function.body = body
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Any
                    CALL 'public final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `delegating constructor calls with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val myClass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
            kind = ClassKind.CLASS
        }
        val constructor = IrFactoryImpl.buildConstructor {
            isPrimary = true
            returnType = myClass.symbol.createType(false, listOf())
        }.apply {
            parent = myClass
        }
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val incorrectDelegatingConstructorCall = IrDelegatingConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.anyType,
            symbol = constructor.symbol,
            typeArgumentsCount = 0
        )

        val correctDelegatingConstructorCall = IrDelegatingConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.unitType,
            symbol = constructor.symbol,
            typeArgumentsCount = 0
        )

        body.statements.addAll(listOf(incorrectDelegatingConstructorCall, correctDelegatingConstructorCall))
        function.body = body
        file.addChild(myClass)
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Any
                    DELEGATING_CONSTRUCTOR_CALL 'public constructor <init> () [primary] declared in org.sample.MyClass'
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `instance initializer calls with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val myClass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
            kind = ClassKind.CLASS
        }
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val incorrectInstanceInitializerCall = IrInstanceInitializerCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            classSymbol = myClass.symbol,
            type = TestIrBuiltins.intType
        )

        val correctInstanceInitializerCall = IrInstanceInitializerCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            classSymbol = myClass.symbol,
            type = TestIrBuiltins.unitType
        )

        body.statements.addAll(listOf(incorrectInstanceInitializerCall, correctInstanceInitializerCall))
        function.body = body
        file.addChild(myClass)
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Int
                    INSTANCE_INITIALIZER_CALL classDescriptor='CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]' type=kotlin.Int
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `return expressions with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.booleanType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val incorrectReturn = IrReturnImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.booleanType,
            returnTargetSymbol = function.symbol,
            value = createTrueConst()
        )

        val correctReturn = IrReturnImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.nothingType,
            returnTargetSymbol = function.symbol,
            value = createTrueConst()
        )

        body.statements.addAll(listOf(incorrectReturn, correctReturn))
        function.body = body
        file.addChild(function)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Nothing, got kotlin.Boolean
                    RETURN type=kotlin.Boolean from='public final fun foo (): kotlin.Boolean declared in org.sample'
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Boolean
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `throw expressions with incorrect type are reported`() {
        val file = createIrFile("test.kt")
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val errorFunction = IrFactoryImpl.buildFun {
            name = Name.identifier("myError")
            returnType = TestIrBuiltins.nothingType
        }
        val body = IrFactoryImpl.createBlockBody(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
        )

        val incorrectThrow = IrThrowImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.anyType,
            value = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.nothingType, errorFunction.symbol)
        )

        val correctThrow = IrThrowImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = TestIrBuiltins.nothingType,
            value = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.nothingType, errorFunction.symbol)
        )

        body.statements.addAll(listOf(incorrectThrow, correctThrow))
        function.body = body
        file.addChild(function)
        file.addChild(errorFunction)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Nothing, got kotlin.Any
                    THROW type=kotlin.Any
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `constructors cannot have dispatch or extension parameters`() {
        val file = createIrFile("test.kt")
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }
        klass.createThisReceiverParameter()
        file.addChild(klass)
        val ctorWithDispatchParameter = klass.addConstructor()
        ctorWithDispatchParameter.addValueParameter {
            name = SpecialNames.THIS
            type = klass.defaultType
        }.apply {
            kind = IrParameterKind.DispatchReceiver
        }
        val ctorWithExtensionParameter = klass.addConstructor()
        ctorWithExtensionParameter.addValueParameter {
            name = SpecialNames.THIS
            type = klass.defaultType
        }.apply {
            kind = IrParameterKind.ExtensionReceiver
        }
        testValidation(
            IrVerificationMode.ERROR,
            file,
            listOf(
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Constructors of non-inner classes can't have dispatch receiver parameters
                    CONSTRUCTOR visibility:public <> (<this>:org.sample.MyClass) returnType:org.sample.MyClass
                      inside CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Constructors can't have extension receiver parameters
                    CONSTRUCTOR visibility:public <> (<this>:org.sample.MyClass) returnType:org.sample.MyClass
                      inside CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            )
        )
    }

    @Test
    fun `functions with body of type IrExpressionBody are reported`() {
        val file = createIrFile("test.kt")
        val functionWithExpressionBody = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.booleanType
        }.apply {
            body = IrFactoryImpl.createExpressionBody(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                expression = createTrueConst()
            )
        }
        file.addChild(functionWithExpressionBody)
        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: IrFunction body cannot be of type IrExpressionBody. Use IrBlockBody instead.
                    FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Boolean
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                )
            ),
        )
    }

    @Test
    fun `elements with invalid offsets are reported`() {
        val file = createIrFile("test.kt")

        val functionWithTooSmallOffsets = IrFactoryImpl.buildFun {
            name = Name.identifier("tooSmall")
            returnType = TestIrBuiltins.unitType
        }.apply {
            body = IrFactoryImpl.createBlockBody(
                startOffset = -3,
                endOffset = 0,
            )
        }

        val functionWithMismatchedNegativeOffsets = IrFactoryImpl.buildFun {
            name = Name.identifier("mismatchNegatives")
            returnType = TestIrBuiltins.unitType
        }.apply {
            body = IrFactoryImpl.createBlockBody(
                startOffset = -1,
                endOffset = 0,
            )
        }

        val functionWithStartGreaterThanEnd = IrFactoryImpl.buildFun {
            name = Name.identifier("startGreater")
            returnType = TestIrBuiltins.unitType
        }.apply {
            body = IrFactoryImpl.createBlockBody(
                startOffset = 4,
                endOffset = 2,
            )
        }

        file.addChild(functionWithTooSmallOffsets)
        file.addChild(functionWithMismatchedNegativeOffsets)
        file.addChild(functionWithStartGreaterThanEnd)

        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Element has invalid offsets. Offsets must be >= 0, UNDEFINED_OFFSET (-1) or SYNTHETIC_OFFSET (-2). Actual: startOffset=-3, endOffset=0
                    BLOCK_BODY
                      inside FUN name:tooSmall visibility:public modality:FINAL <> () returnType:kotlin.Unit
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Element has invalid offsets. UNDEFINED_OFFSET (-1) and SYNTHETIC_OFFSET (-2) can only appear simultaneously in both startOffset and endOffset. Actual: startOffset=-1, endOffset=0
                    BLOCK_BODY
                      inside FUN name:mismatchNegatives visibility:public modality:FINAL <> () returnType:kotlin.Unit
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Element has invalid offsets. startOffset must not be greater than endOffset Actual: startOffset=4, endOffset=2
                    BLOCK_BODY
                      inside FUN name:startGreater visibility:public modality:FINAL <> () returnType:kotlin.Unit
                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 5, null),
                ),
            ),
        )
    }

    @Test
    fun `nested offset ranges violations are reported`() {
        class Context(
            val offsetRangeStart: Int, val offsetRangeEnd: Int,
            val parent: IrDeclarationParent,
            val container: IrElement,
        ) {
            fun buildFun(
                start: Int,
                end: Int,
                block: (Context.() -> Unit)? = null,
            ) {
                fun StringBuilder.appendOffset(offset: Int) = if (offset < 0) append('m').append(-offset) else append(offset)
                val funName = buildString {
                    append("f_").appendOffset(start).append("_").appendOffset(end)
                }
                val function = IrFactoryImpl.buildFun builder@{
                    this@builder.name = Name.identifier(funName)
                    this@builder.startOffset = start
                    this@builder.endOffset = end
                    this@builder.returnType = TestIrBuiltins.unitType
                }
                val body = IrFactoryImpl.createBlockBody(-1, -1) // Use synthetic offsets to bypass this block during the validation.
                function.body = body

                function.setDeclarationsParent(parent)
                addToContainer(function)

                if (block != null) {
                    Context(
                        offsetRangeStart = start,
                        offsetRangeEnd = end,
                        parent = function,
                        container = body
                    ).block()
                }
            }

            fun buildInlinedFunBlock(
                start: Int,
                end: Int,
                block: (Context.() -> Unit),
            ) {
                val inlinedFunBlock = IrInlinedFunctionBlockImpl(
                    startOffset = -1, // Use synthetic offsets to bypass this block during the validation.
                    endOffset = -1,
                    type = TestIrBuiltins.unitType,
                    inlinedFunctionSymbol = null,
                    inlinedFunctionStartOffset = start,
                    inlinedFunctionEndOffset = end,
                    inlinedFunctionFileEntry = NaiveSourceBasedFileEntryImpl(name = "dummy"),
                )
                addToContainer(inlinedFunBlock)

                Context(
                    offsetRangeStart = start,
                    offsetRangeEnd = end,
                    parent = this.parent,
                    container = inlinedFunBlock
                ).block()
            }

            private fun addToContainer(statement: IrStatement) {
                when (container) {
                    is IrDeclarationContainer -> container.declarations += statement as IrDeclaration
                    is IrStatementContainer -> container.statements += statement
                    else -> error("Unexpected container type: ${container::class.java}")
                }
            }
        }

        val file = createIrFile(maxOffset = 2000)
        val fileContext = Context(
            offsetRangeStart = file.startOffset,
            offsetRangeEnd = file.endOffset,
            parent = file,
            container = file,
        )

        with(fileContext) {
            // Top-level entities with any non-violating offsets.
            buildFun(start = -1, end = -1)
            buildFun(start = -2, end = -2)
            buildFun(start = 0, end = 0)
            buildFun(start = file.endOffset, end = file.endOffset)
            buildFun(start = 0, end = file.endOffset)
            buildFun(start = 1, end = file.endOffset - 1)

            // Top-level entity that violates offsets.
            buildFun(start = 0, end = file.endOffset + 1)

            buildFun(start = 100, end = 200) {
                val topLevelFunContext = this

                // Nested entities, which do not violate offsets.
                buildFun(start = -1, end = -1)
                buildFun(start = -2, end = -2)
                buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeStart)
                buildFun(start = topLevelFunContext.offsetRangeEnd, end = topLevelFunContext.offsetRangeEnd)
                buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeEnd)
                buildFun(start = topLevelFunContext.offsetRangeStart + 1, end = topLevelFunContext.offsetRangeEnd - 1)

                // Nested entities, which violate offsets.
                buildFun(start = topLevelFunContext.offsetRangeStart - 1, end = topLevelFunContext.offsetRangeEnd)
                buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeEnd + 1)

                // Nested entities, which transitively violate offsets.
                buildFun(start = -1, end = -1) {
                    buildFun(start = -2, end = -2) {
                        buildFun(start = topLevelFunContext.offsetRangeStart - 1, end = topLevelFunContext.offsetRangeEnd)
                        buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeEnd + 1)

                        buildFun(start = topLevelFunContext.offsetRangeStart + 10, end = topLevelFunContext.offsetRangeEnd - 10) {
                            val nestedClassContext = this
                            buildFun(start = nestedClassContext.offsetRangeStart - 1, end = nestedClassContext.offsetRangeEnd)
                            buildFun(start = nestedClassContext.offsetRangeStart, end = nestedClassContext.offsetRangeEnd + 1)
                        }
                    }
                }
            }

            buildFun(start = 300, end = 400) {
                val topLevelFunContext = this

                buildInlinedFunBlock(start = -1, end = -1) {
                    // Nested entities, which do not violate offsets.
                    buildFun(start = -1, end = -1)
                    buildFun(start = -2, end = -2)
                    buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeStart)
                    buildFun(start = topLevelFunContext.offsetRangeEnd, end = topLevelFunContext.offsetRangeEnd)
                    buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeEnd)
                    buildFun(start = topLevelFunContext.offsetRangeStart + 1, end = topLevelFunContext.offsetRangeEnd - 1)

                    // Nested entities, which violate offsets.
                    buildFun(start = topLevelFunContext.offsetRangeStart - 1, end = topLevelFunContext.offsetRangeEnd)
                    buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeEnd + 1)

                    // Nested entities, which transitively violate offsets.
                    buildFun(start = -1, end = -1) {
                        buildFun(start = -2, end = -2) {
                            buildFun(start = topLevelFunContext.offsetRangeStart - 1, end = topLevelFunContext.offsetRangeEnd)
                            buildFun(start = topLevelFunContext.offsetRangeStart, end = topLevelFunContext.offsetRangeEnd + 1)

                            buildFun(start = topLevelFunContext.offsetRangeStart + 10, end = topLevelFunContext.offsetRangeEnd - 10) {
                                val nestedClassContext = this
                                buildFun(start = nestedClassContext.offsetRangeStart - 1, end = nestedClassContext.offsetRangeEnd)
                                buildFun(start = nestedClassContext.offsetRangeStart, end = nestedClassContext.offsetRangeEnd + 1)
                            }
                        }
                    }
                }
            }

            buildFun(start = 500, end = 600) {
                buildInlinedFunBlock(start = 700, end = 800) {
                    val inlinedBlockContext = this

                    // Nested entities, which do not violate offsets.
                    buildFun(start = -1, end = -1)
                    buildFun(start = -2, end = -2)
                    buildFun(start = inlinedBlockContext.offsetRangeStart, end = inlinedBlockContext.offsetRangeStart)
                    buildFun(start = inlinedBlockContext.offsetRangeEnd, end = inlinedBlockContext.offsetRangeEnd)
                    buildFun(start = inlinedBlockContext.offsetRangeStart, end = inlinedBlockContext.offsetRangeEnd)
                    buildFun(start = inlinedBlockContext.offsetRangeStart + 1, end = inlinedBlockContext.offsetRangeEnd - 1)

                    // Nested entities, which violate offsets.
                    buildFun(start = inlinedBlockContext.offsetRangeStart - 1, end = inlinedBlockContext.offsetRangeEnd)
                    buildFun(start = inlinedBlockContext.offsetRangeStart, end = inlinedBlockContext.offsetRangeEnd + 1)

                    // Nested entities, which transitively violate offsets.
                    buildFun(start = -1, end = -1) {
                        buildFun(start = -2, end = -2) {
                            buildFun(start = inlinedBlockContext.offsetRangeStart - 1, end = inlinedBlockContext.offsetRangeEnd)
                            buildFun(start = inlinedBlockContext.offsetRangeStart, end = inlinedBlockContext.offsetRangeEnd + 1)

                            buildFun(start = inlinedBlockContext.offsetRangeStart + 10, end = inlinedBlockContext.offsetRangeEnd - 10) {
                                val nestedClassContext = this
                                buildFun(start = nestedClassContext.offsetRangeStart - 1, end = nestedClassContext.offsetRangeEnd)
                                buildFun(start = nestedClassContext.offsetRangeStart, end = nestedClassContext.offsetRangeEnd + 1)
                            }
                        }
                    }
                }
            }
        }

        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [0:2001] is not within the outer offsets range [0:2000] (owner = FILE fqName:org.sample fileName:test.kt)
                    FUN name:f_0_2001 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside FILE fqName:org.sample fileName:test.kt
                                        """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 1, 1, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [99:200] is not within the outer offsets range [100:200] (owner = FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_99_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 75, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [100:201] is not within the outer offsets range [100:200] (owner = FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_100_201 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 76, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [99:200] is not within the outer offsets range [100:200] (owner = FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_99_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 75, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [100:201] is not within the outer offsets range [100:200] (owner = FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_100_201 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 76, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [109:190] is not within the outer offsets range [110:190] (owner = FUN name:f_110_190 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_109_190 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_110_190 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside BLOCK_BODY
                                    inside FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 85, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [110:191] is not within the outer offsets range [110:190] (owner = FUN name:f_110_190 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_110_191 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_110_190 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside BLOCK_BODY
                                    inside FUN name:f_100_200 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 86, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [299:400] is not within the outer offsets range [300:400] (owner = FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_299_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside INLINED_BLOCK type=kotlin.Unit origin=null
                        inside BLOCK_BODY
                          inside FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 275, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [300:401] is not within the outer offsets range [300:400] (owner = FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_300_401 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside INLINED_BLOCK type=kotlin.Unit origin=null
                        inside BLOCK_BODY
                          inside FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 276, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [299:400] is not within the outer offsets range [300:400] (owner = FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_299_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside INLINED_BLOCK type=kotlin.Unit origin=null
                                inside BLOCK_BODY
                                  inside FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                    inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 275, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [300:401] is not within the outer offsets range [300:400] (owner = FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_300_401 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside INLINED_BLOCK type=kotlin.Unit origin=null
                                inside BLOCK_BODY
                                  inside FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                    inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 276, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [309:390] is not within the outer offsets range [310:390] (owner = FUN name:f_310_390 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_309_390 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_310_390 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside INLINED_BLOCK type=kotlin.Unit origin=null
                                    inside BLOCK_BODY
                                      inside FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 285, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [310:391] is not within the outer offsets range [310:390] (owner = FUN name:f_310_390 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_310_391 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_310_390 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside INLINED_BLOCK type=kotlin.Unit origin=null
                                    inside BLOCK_BODY
                                      inside FUN name:f_300_400 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 286, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [699:800] is not within the outer offsets range [700:800] (owner = INLINED_BLOCK type=kotlin.Unit origin=null)
                    FUN name:f_699_800 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside INLINED_BLOCK type=kotlin.Unit origin=null
                        inside BLOCK_BODY
                          inside FUN name:f_500_600 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 675, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [700:801] is not within the outer offsets range [700:800] (owner = INLINED_BLOCK type=kotlin.Unit origin=null)
                    FUN name:f_700_801 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside INLINED_BLOCK type=kotlin.Unit origin=null
                        inside BLOCK_BODY
                          inside FUN name:f_500_600 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 676, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [699:800] is not within the outer offsets range [700:800] (owner = INLINED_BLOCK type=kotlin.Unit origin=null)
                    FUN name:f_699_800 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside INLINED_BLOCK type=kotlin.Unit origin=null
                                inside BLOCK_BODY
                                  inside FUN name:f_500_600 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                    inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 675, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [700:801] is not within the outer offsets range [700:800] (owner = INLINED_BLOCK type=kotlin.Unit origin=null)
                    FUN name:f_700_801 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside INLINED_BLOCK type=kotlin.Unit origin=null
                                inside BLOCK_BODY
                                  inside FUN name:f_500_600 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                    inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 676, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [709:790] is not within the outer offsets range [710:790] (owner = FUN name:f_710_790 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_709_790 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_710_790 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside INLINED_BLOCK type=kotlin.Unit origin=null
                                    inside BLOCK_BODY
                                      inside FUN name:f_500_600 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 685, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: The offsets range [710:791] is not within the outer offsets range [710:790] (owner = FUN name:f_710_790 visibility:public modality:FINAL <> () returnType:kotlin.Unit)
                    FUN name:f_710_791 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside BLOCK_BODY
                        inside FUN name:f_710_790 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside BLOCK_BODY
                            inside FUN name:f_m2_m2 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                              inside BLOCK_BODY
                                inside FUN name:f_m1_m1 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                  inside INLINED_BLOCK type=kotlin.Unit origin=null
                                    inside BLOCK_BODY
                                      inside FUN name:f_500_600 visibility:public modality:FINAL <> () returnType:kotlin.Unit
                                        inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(file.fileEntry.name, 3, 686, null),
                ),
            )
        )
    }

    @Test
    fun `unbound symbols are reported`() {
        val file = createIrFile("test.kt")

        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }.apply {
            sealedSubclasses = listOf(IrClassSymbolImpl())
        }

        val function1 = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }.apply {
            overriddenSymbols = listOf(IrSimpleFunctionSymbolImpl())
        }

        val function2 = IrFactoryImpl.buildFun {
            name = Name.identifier("bar")
            returnType = TestIrBuiltins.unitType
        }.apply {
            correspondingPropertySymbol = IrPropertySymbolImpl()
        }

        val function3 = IrFactoryImpl.buildFun {
            name = Name.identifier("baz")
            returnType = IrSimpleTypeImpl(IrClassSymbolImpl(), SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList())
        }

        val property = IrFactoryImpl.buildProperty {
            name = Name.identifier("p")
        }.apply {
            overriddenSymbols = listOf(IrPropertySymbolImpl())
        }

        file.addChild(klass)
        file.addChild(function1)
        file.addChild(function2)
        file.addChild(function3)
        file.addChild(property)

        testValidation(
            IrVerificationMode.WARNING,
            file,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Unexpected unbound symbol
                    CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Unexpected unbound symbol
                    FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Unexpected unbound symbol
                    FUN name:bar visibility:public modality:FINAL <> () returnType:kotlin.Unit
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Unexpected unbound symbol
                    FUN name:baz visibility:public modality:FINAL <> () returnType:<unbound IrClassSymbolImpl>
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Unexpected unbound symbol
                    PROPERTY name:p visibility:public modality:FINAL [val]
                      inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create(null, 0, 0, null),
                ),
            ),
        )
    }
}
