/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val fakeLocation = Location(HeaderId("doesntmatter"))

private class FakeObjCClass(name: String) : ObjCClass(name) {
    override val location: Location = fakeLocation
    override val protocols: List<ObjCProtocol> = emptyList()
    override val methods: List<ObjCMethod> = emptyList()
    override val properties: List<ObjCProperty> = emptyList()
    override val isForwardDeclaration: Boolean = false
    override val binaryName: String? = null
    override val baseClass: ObjCClass? = null
}

private class FakeStructDecl(spelling: String) : StructDecl(spelling) {
    override val def: StructDef? = null

    override val location: Location = fakeLocation
}
class ManglingSmokeTests {

    private val mangler: InteropMangler = KotlinLikeInteropMangler()

    private val nsStringClass = FakeObjCClass("NSString")

    private val int32Type = IntegerType(32, true, "int32_t")

    @Test
    fun `typedef should not affect mangling`() {

        val cBoolTypedef = Typedef(TypedefDef(CBoolType, "MyBool", fakeLocation))

        val functions = listOf(
                FunctionDecl("a", listOf(Parameter("a", CBoolType, false)), CBoolType, "", false, false),
                FunctionDecl("a", listOf(Parameter("b", CBoolType, false)), cBoolTypedef, "", false, false),
                FunctionDecl("a", listOf(Parameter("a", cBoolTypedef, false)), CBoolType, "", false, false),
                FunctionDecl("a", listOf(Parameter("a", cBoolTypedef, false)), cBoolTypedef, "", false, false)
        )
        with (mangler) {
            functions.reduce { left, right ->
                assertEquals(left.uniqueSymbolName, right.uniqueSymbolName)
                left
            }
        }
    }

    @Test
    fun `mangling should not depend on parameter names`() {

        val functionDeclarationA = FunctionDecl("a", listOf(Parameter("a", CBoolType, false)), CBoolType, "", false, false)
        val functionDeclarationB = FunctionDecl("a", listOf(Parameter("b", CBoolType, false)), CBoolType, "", false, false)
        with (mangler) {
            assertEquals(functionDeclarationA.uniqueSymbolName, functionDeclarationB.uniqueSymbolName)
        }
    }

    @Test
    fun `parameter type name embedded in function name`() {
        val functionDeclarationA = FunctionDecl("a", listOf(Parameter("a", CharType, false)), CBoolType, "", false, false)
        val functionDeclarationB = FunctionDecl("achar", emptyList(), CBoolType, "", false, false)
        with (mangler) {
            assertNotEquals(functionDeclarationA.uniqueSymbolName, functionDeclarationB.uniqueSymbolName)
        }
    }

    @Test
    fun `objc methods with same names but different parameters`() {
        val nsStringPtrType = ObjCObjectPointer(nsStringClass, ObjCPointer.Nullability.Nullable, listOf())

        val methodA = ObjCMethod("name::", "v28@0:8@16i24",
                listOf(Parameter("name", nsStringPtrType, false), Parameter("age", int32Type, false)), VoidType,
                false, false, false, false, false, false, false)

        val methodB = ObjCMethod("name:", "v20@0:8i16",
                listOf(Parameter("age", int32Type, false)), VoidType,
                false, false, false, false, false, false, false)

        with (mangler) {
            assertNotEquals(methodA.uniqueSymbolName, methodB.uniqueSymbolName)
        }
    }

    @Test
    fun `objc methods with same names but different parameter names`() {
        val nsStringPtrType = ObjCObjectPointer(nsStringClass, ObjCPointer.Nullability.Nullable, listOf())

        val methodA = ObjCMethod("desc:a:", "v28@0:8@16i24",
                listOf(Parameter("name", int32Type, false), Parameter("a", int32Type, false)), VoidType,
                false, false, false, false, false, false, false)

        val methodB = ObjCMethod("desc:b:", "v28@0:8@16i24",
                listOf(Parameter("name", int32Type, false), Parameter("b", int32Type, false)), VoidType,
                false, false, false, false, false, false, false)

        with (mangler) {
            assertNotEquals(methodA.uniqueSymbolName, methodB.uniqueSymbolName)
        }
    }

    @Test
    fun `struct smoke`() {
        val structA = FakeStructDecl("struct_name")
        val structB = FakeStructDecl("structName")
        with (mangler) {
            assertNotEquals(structA.uniqueSymbolName, structB.uniqueSymbolName)
        }
    }

    @Test
    fun `constants smoke`() {
        val constant = IntegerConstantDef("DEBUG", CBoolType, 1)
        val macro = WrappedMacroDef("DEBUG", CBoolType)
        with (mangler) {
            assertEquals(constant.uniqueSymbolName, macro.uniqueSymbolName)
        }
    }

    @Test
    fun `different modules`() {
        val moduleA = ManglingContext.Module("A")
        val moduleB = ManglingContext.Module("B")

        val manglerA = KotlinLikeInteropMangler(moduleA)
        val manglerB = KotlinLikeInteropMangler(moduleB)

        val declaration = WrappedMacroDef("DEBUG", CBoolType)

        assertNotEquals(
                with(manglerA) { declaration.uniqueSymbolName },
                with(manglerB) { declaration.uniqueSymbolName }
        )
    }

    @Test
    fun `different classes`() {
        val module = ManglingContext.Module("Foundation")

        val classA = ManglingContext.Entity("NSArray", module)
        val classB = ManglingContext.Entity("NSMutableArray", module)

        val manglerA = KotlinLikeInteropMangler(classA)
        val manglerB = KotlinLikeInteropMangler(classB)

        val getter = ObjCMethod("size", "Q16@0:8", emptyList(), IntegerType(64, false, "unsigned long"),
                false, false, false, false, false, false, false)
        val property = ObjCProperty("size", getter, null)

        assertNotEquals(
                with(manglerA) { property.uniqueSymbolName },
                with(manglerB) { property.uniqueSymbolName }
        )
    }
}