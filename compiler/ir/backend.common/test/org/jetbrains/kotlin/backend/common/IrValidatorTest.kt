/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.utils.TestMessageCollector
import org.jetbrains.kotlin.test.utils.TestMessageCollector.Message
import kotlin.reflect.KProperty
import kotlin.test.*

class IrValidatorTest {

    private lateinit var messageCollector: TestMessageCollector
    private lateinit var module: IrModuleFragment

    @BeforeTest
    fun setUp() {
        messageCollector = TestMessageCollector()

        val moduleDescriptor = ModuleDescriptorImpl(
            Name.special("<testModule>"),
            LockBasedStorageManager("IrValidatorTest"),
            DefaultBuiltIns.Instance
        )
        module = IrModuleFragmentImpl(moduleDescriptor, TestIrBuiltins)
    }

    private fun buildInvalidIrExpressionWithNoLocations(): IrElement {
        val stringConcatenationWithWrongType = IrStringConcatenationImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType)

        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.anyType
        }
        val functionCall =
            IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, TestIrBuiltins.anyType, function.symbol, 0, 1).apply {
                dispatchReceiver = stringConcatenationWithWrongType
                putValueArgument(0, stringConcatenationWithWrongType)
            }
        return functionCall
    }

    private fun createIrFile(name: String = "test.kt", packageFqName: FqName = FqName("org.sample")): IrFile {
        val fileEntry = NaiveSourceBasedFileEntryImpl(name, lineStartOffsets = intArrayOf(0, 10, 25), maxOffset = 75)
        return IrFileImpl(fileEntry, IrFileSymbolImpl(), packageFqName).also(module::addFile)
    }

    private fun buildInvalidIrTreeWithLocations(): IrElement {
        val file = createIrFile()
        val function = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
        }
        val body = IrFactoryImpl.createBlockBody(5, 24)
        val stringConcatenationWithWrongType = IrStringConcatenationImpl(9, 20, TestIrBuiltins.anyType)
        val functionCall =
            IrCallImpl(6, 23, TestIrBuiltins.anyType, function.symbol, 0, 1).apply {
                dispatchReceiver = stringConcatenationWithWrongType
                putValueArgument(0, stringConcatenationWithWrongType)
            }
        body.statements.add(functionCall)
        function.body = body
        file.addChild(function)
        return file
    }

    private inline fun runValidationAndAssert(mode: IrVerificationMode, block: () -> Unit) {
        if (mode == IrVerificationMode.ERROR) {
            assertFailsWith<IrValidationError>(block = block)
        } else {
            block()
        }
    }

    private fun testValidation(mode: IrVerificationMode, tree: IrElement, expectedMessages: List<Message>) {
        runValidationAndAssert(mode) {
            validateIr(messageCollector, mode) {
                performBasicIrValidation(
                    tree,
                    TestIrBuiltins,
                    phaseName = "IrValidatorTest",
                    checkTypes = true,
                    checkValueScopes = true,
                    checkTypeParameterScopes = true,
                    checkCrossFileFieldUsage = true,
                    checkVisibilities = true,
                )
                assertEquals(expectedMessages, messageCollector.messages)
            }
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
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.String, got kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Any declared in <no parent>' type=kotlin.Any origin=null
                    """.trimIndent(),
                    null,
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Any declared in <no parent>' type=kotlin.Any origin=null
                    """.trimIndent(),
                    null,
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
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Any
                    CALL 'public final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 7, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.String, got kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
            ),
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
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.String, got kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Any declared in <no parent>' type=kotlin.Any origin=null
                    """.trimIndent(),
                    null,
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Any declared in <no parent>' type=kotlin.Any origin=null
                    """.trimIndent(),
                    null,
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
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.Unit, got kotlin.Any
                    CALL 'public final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 7, null),
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected kotlin.String, got kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=kotlin.Any
                    STRING_CONCATENATION type=kotlin.Any
                      inside CALL 'public final fun foo (): kotlin.Unit declared in org.sample' type=kotlin.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
            ),
        )
    }

    @Test
    fun `no infinite recursion if there are cycles in IR`() {
        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }
        klass.declarations.add(klass)
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
                    null
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
        testValidation(
            IrVerificationMode.WARNING,
            klass,
            listOf(
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Declarations with wrong parent: 1
                    declaration: FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                    expectedParent: FUN name:foo visibility:public modality:FINAL <> () returnType:kotlin.Unit
                    actualParent: CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                    Expected parents:
                    
                    CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]""".trimIndent(),
                    null,
                )
            ),
        )
    }

    @Test
    fun `private declarations can't be referenced from a different file`() {
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
    fun `annotations are ignored by IR visibility checker`() {
        val file1 = createIrFile("MyAnnotation.kt")
        val annotationClass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyAnnotation")
            visibility = DescriptorVisibilities.PRIVATE
            kind = ClassKind.ANNOTATION_CLASS
        }
        file1.addChild(annotationClass)
        val annotationConstructor = IrFactoryImpl.createConstructor(
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
        annotationClass.addChild(annotationConstructor)
        val file2 = createIrFile("b.kt")
        val annotatedClass = IrFactoryImpl.buildClass {
            name = Name.identifier("AnnotatedClass")
        }
        annotatedClass.annotations = listOf(
            IrConstructorCallImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = IrSimpleTypeImpl(annotationClass.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()),
                symbol = annotationConstructor.symbol,
                typeArgumentsCount = 0,
                constructorTypeArgumentsCount = 0,
                valueArgumentsCount = 0,
            )
        )
        file2.addChild(annotatedClass)
        testValidation(IrVerificationMode.WARNING, file2, emptyList())
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
                        inside FIELD name:myField type:kotlin.Any visibility:public
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 2, 4, null),
                )
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
                    FIELD name:myField type:E of org.sample.MyClass visibility:public
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
        outerClass.thisReceiver = buildValueParameter(outerClass) {
            type = outerType
            name = SpecialNames.THIS
        }
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
            )
        )
    }
}

private object TestIrBuiltins : IrBuiltIns() {
    private val builtinsPackage = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(), FqName("kotlin"))

    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    override val irFactory: IrFactory
        get() = IrFactoryImpl

    override val anyClass: IrClassSymbol by builtinClass("Any")
    override val anyType: IrType by builtinType(anyClass)
    override val anyNType: IrType by builtinType(anyClass, nullable = true)
    override val booleanClass: IrClassSymbol by builtinClass("Boolean")
    override val booleanType: IrType by builtinType(booleanClass)
    override val charClass: IrClassSymbol by builtinClass("Char")
    override val charType: IrType by builtinType(charClass)
    override val numberClass: IrClassSymbol by builtinClass("Number")
    override val numberType: IrType by builtinType(numberClass)
    override val byteClass: IrClassSymbol by builtinClass("Byte")
    override val byteType: IrType by builtinType(byteClass)
    override val shortClass: IrClassSymbol by builtinClass("Short")
    override val shortType: IrType by builtinType(shortClass)
    override val intClass: IrClassSymbol by builtinClass("Int")
    override val intType: IrType by builtinType(intClass)
    override val longClass: IrClassSymbol by builtinClass("Long")
    override val longType: IrType by builtinType(longClass)
    override val floatClass: IrClassSymbol by builtinClass("Float")
    override val floatType: IrType by builtinType(floatClass)
    override val doubleClass: IrClassSymbol by builtinClass("Double")
    override val doubleType: IrType by builtinType(doubleClass)
    override val nothingClass: IrClassSymbol by builtinClass("Nothing")
    override val nothingType: IrType by builtinType(nothingClass)
    override val nothingNType: IrType by builtinType(nothingClass)
    override val unitClass: IrClassSymbol by builtinClass("Unit")
    override val unitType: IrType by builtinType(unitClass)
    override val stringClass: IrClassSymbol by builtinClass("String")
    override val stringType: IrType by builtinType(stringClass)
    override val charSequenceClass: IrClassSymbol by builtinClass("CharSequence")
    override val collectionClass: IrClassSymbol by builtinClass("Collection")
    override val arrayClass: IrClassSymbol by builtinClass("Array")
    override val setClass: IrClassSymbol by builtinClass("Set")
    override val listClass: IrClassSymbol by builtinClass("List")
    override val mapClass: IrClassSymbol by builtinClass("Map")
    override val mapEntryClass: IrClassSymbol by builtinClass("Entry")
    override val iterableClass: IrClassSymbol by builtinClass("Iterable")
    override val iteratorClass: IrClassSymbol by builtinClass("Iterator")
    override val listIteratorClass: IrClassSymbol by builtinClass("ListIterator")
    override val mutableCollectionClass: IrClassSymbol by builtinClass("MutableCollection")
    override val mutableSetClass: IrClassSymbol by builtinClass("MutableSet")
    override val mutableListClass: IrClassSymbol by builtinClass("MutableList")
    override val mutableMapClass: IrClassSymbol by builtinClass("MutableMap")
    override val mutableMapEntryClass: IrClassSymbol by builtinClass("Entry")
    override val mutableIterableClass: IrClassSymbol by builtinClass("MutableIterable")
    override val mutableIteratorClass: IrClassSymbol by builtinClass("MutableIterator")
    override val mutableListIteratorClass: IrClassSymbol by builtinClass("MutableListIterator")
    override val comparableClass: IrClassSymbol by builtinClass("Comparable")
    override val throwableClass: IrClassSymbol by builtinClass("Throwable")
    override val throwableType: IrType by builtinType(throwableClass)
    override val kCallableClass: IrClassSymbol by builtinClass("KCallable")
    override val kPropertyClass: IrClassSymbol by builtinClass("KProperty")
    override val kClassClass: IrClassSymbol by builtinClass("KClass")
    override val kTypeClass: IrClassSymbol by builtinClass("KType")
    override val kProperty0Class: IrClassSymbol by builtinClass("KProperty0")
    override val kProperty1Class: IrClassSymbol by builtinClass("KProperty1")
    override val kProperty2Class: IrClassSymbol by builtinClass("KProperty2")
    override val kMutableProperty0Class: IrClassSymbol by builtinClass("KMutableProperty0")
    override val kMutableProperty1Class: IrClassSymbol by builtinClass("KMutableProperty1")
    override val kMutableProperty2Class: IrClassSymbol by builtinClass("KMutableProperty2")
    override val functionClass: IrClassSymbol by builtinClass("Function")
    override val kFunctionClass: IrClassSymbol by builtinClass("KFunction")
    override val annotationClass: IrClassSymbol by builtinClass("Annotation")
    override val annotationType: IrType by builtinType(annotationClass)

    override val primitiveTypeToIrType: Map<PrimitiveType, IrType> = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.LONG to longType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.DOUBLE to doubleType
    )

    override val primitiveIrTypes: List<IrType>
        get() = missingBuiltIn()
    override val primitiveIrTypesWithComparisons: List<IrType>
        get() = missingBuiltIn()
    override val primitiveFloatingPointIrTypes: List<IrType>
        get() = missingBuiltIn()

    override val byteIterator: IrClassSymbol by builtinClass("ByteIterator")
    override val charIterator: IrClassSymbol by builtinClass("CharIterator")
    override val shortIterator: IrClassSymbol by builtinClass("ShortIterator")
    override val intIterator: IrClassSymbol by builtinClass("IntIterator")
    override val longIterator: IrClassSymbol by builtinClass("LongIterator")
    override val floatIterator: IrClassSymbol by builtinClass("FloatIterator")
    override val doubleIterator: IrClassSymbol by builtinClass("DoubleIterator")
    override val booleanIterator: IrClassSymbol by builtinClass("BooleanIterator")
    override val byteArray: IrClassSymbol by builtinClass("ByteArray")
    override val charArray: IrClassSymbol by builtinClass("CharArray")
    override val shortArray: IrClassSymbol by builtinClass("ShortArray")
    override val intArray: IrClassSymbol by builtinClass("IntArray")
    override val longArray: IrClassSymbol by builtinClass("LongArray")
    override val floatArray: IrClassSymbol by builtinClass("FloatArray")
    override val doubleArray: IrClassSymbol by builtinClass("DoubleArray")
    override val booleanArray: IrClassSymbol by builtinClass("BooleanArray")

    override val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType>
        get() = missingBuiltIn()
    override val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, IrClassSymbol>
        get() = missingBuiltIn()
    override val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?>
        get() = missingBuiltIn()
    override val primitiveArrayForType: Map<IrType?, IrClassSymbol>
        get() = missingBuiltIn()
    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol>
        get() = missingBuiltIn()
    override val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?>
        get() = missingBuiltIn()
    override val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val booleanNotSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val eqeqeqSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val eqeqSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val throwCceSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val throwIseSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val andandSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val ororSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val checkNotNullSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()

    override val enumClass: IrClassSymbol by builtinClass("Enum")

    override val intPlusSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val intTimesSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val intXorSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val extensionToString: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val memberToString: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val extensionStringPlus: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val memberStringPlus: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val arrayOf: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val arrayOfNulls: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val linkageErrorSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()

    override fun functionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun kFunctionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun suspendFunctionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun kSuspendFunctionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun findFunctions(name: Name, vararg packageNameSegments: String): Iterable<IrSimpleFunctionSymbol> {
        missingBuiltIn()
    }

    override fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol> {
        missingBuiltIn()
    }

    override fun findProperties(name: Name, packageFqName: FqName): Iterable<IrPropertySymbol> {
        missingBuiltIn()
    }

    override fun findClass(name: Name, vararg packageNameSegments: String): IrClassSymbol? {
        missingBuiltIn()
    }

    override fun findClass(name: Name, packageFqName: FqName): IrClassSymbol? {
        missingBuiltIn()
    }

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol {
        missingBuiltIn()
    }

    override fun findBuiltInClassMemberFunctions(builtInClass: IrClassSymbol, name: Name): Iterable<IrSimpleFunctionSymbol> {
        missingBuiltIn()
    }

    override fun getNonBuiltInFunctionsByExtensionReceiver(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        missingBuiltIn()
    }

    override fun getNonBuiltinFunctionsByReturnType(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        missingBuiltIn()
    }

    override fun getBinaryOperator(name: Name, lhsType: IrType, rhsType: IrType): IrSimpleFunctionSymbol {
        missingBuiltIn()
    }

    override fun getUnaryOperator(name: Name, receiverType: IrType): IrSimpleFunctionSymbol {
        missingBuiltIn()
    }

    override val operatorsPackageFragment: IrExternalPackageFragment
        get() = missingBuiltIn()
    override val kotlinInternalPackageFragment: IrExternalPackageFragment
        get() = missingBuiltIn()

    private fun builtinClass(name: String) = object {
        val klass = irFactory.buildClass {
            this.name = Name.identifier(name)
        }.apply {
            parent = builtinsPackage
        }

        operator fun getValue(thisRef: TestIrBuiltins, property: KProperty<*>): IrClassSymbol {
            return klass.symbol
        }
    }

    private fun builtinType(klass: IrClassSymbol, nullable: Boolean = false) = object {
        val type = IrSimpleTypeImpl(klass, SimpleTypeNullability.fromHasQuestionMark(nullable), emptyList(), emptyList())
        operator fun getValue(thisRef: TestIrBuiltins, property: KProperty<*>): IrType {
            return type
        }
    }

    private fun missingBuiltIn(): Nothing = fail("Missing built-in")
}
