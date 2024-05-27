/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.utils.TestMessageCollector
import org.jetbrains.kotlin.test.utils.TestMessageCollector.Message
import kotlin.reflect.KProperty
import kotlin.test.*

class IrValidatorTest {

    private lateinit var messageCollector: TestMessageCollector

    @BeforeTest
    fun setUp() {
        messageCollector = TestMessageCollector()
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

    private fun buildInvalidIrTreeWithLocations(): IrElement {
        val fileEntry = NaiveSourceBasedFileEntryImpl("test.kt", lineStartOffsets = intArrayOf(0, 10, 25), maxOffset = 75)
        val file = IrFileImpl(fileEntry, IrFileSymbolImpl(), FqName("org.sample"))
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
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected <uninitialized parent>.String, got <uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Any declared in <no parent>' type=<uninitialized parent>.Any origin=null
                    """.trimIndent(),
                    null,
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=<uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Any declared in <no parent>' type=<uninitialized parent>.Any origin=null
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
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected <uninitialized parent>.Unit, got <uninitialized parent>.Any
                    CALL 'public final fun foo (): <uninitialized parent>.Unit declared in org.sample' type=<uninitialized parent>.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 7, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected <uninitialized parent>.String, got <uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Unit declared in org.sample' type=<uninitialized parent>.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
                Message(
                    WARNING,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=<uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Unit declared in org.sample' type=<uninitialized parent>.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
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
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected <uninitialized parent>.String, got <uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Any declared in <no parent>' type=<uninitialized parent>.Any origin=null
                    """.trimIndent(),
                    null,
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=<uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Any declared in <no parent>' type=<uninitialized parent>.Any origin=null
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
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected <uninitialized parent>.Unit, got <uninitialized parent>.Any
                    CALL 'public final fun foo (): <uninitialized parent>.Unit declared in org.sample' type=<uninitialized parent>.Any origin=null
                      inside BLOCK_BODY
                        inside FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
                          inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 7, null),
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: unexpected type: expected <uninitialized parent>.String, got <uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Unit declared in org.sample' type=<uninitialized parent>.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
                            inside FILE fqName:org.sample fileName:test.kt
                    """.trimIndent(),
                    CompilerMessageLocation.create("test.kt", 1, 10, null),
                ),
                Message(
                    ERROR,
                    """
                    [IR VALIDATION] IrValidatorTest: Duplicate IR node: STRING_CONCATENATION type=<uninitialized parent>.Any
                    STRING_CONCATENATION type=<uninitialized parent>.Any
                      inside CALL 'public final fun foo (): <uninitialized parent>.Unit declared in org.sample' type=<uninitialized parent>.Any origin=null
                        inside BLOCK_BODY
                          inside FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
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
                    declaration: FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
                    expectedParent: FUN name:foo visibility:public modality:FINAL <> () returnType:<uninitialized parent>.Unit
                    actualParent: CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]
                    Expected parents:
                    
                    CLASS CLASS name:MyClass modality:FINAL visibility:public superTypes:[]""".trimIndent(),
                    null,
                )
            ),
        )
    }
}

private object TestIrBuiltins : IrBuiltIns() {

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
