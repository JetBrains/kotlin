/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.Difference
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacades
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.hashCode
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * Checks that [DifferenceCalculatorForPackageFacades] with single-item lists returns the same [Difference] as
 * [DifferenceCalculatorForPackageFacade].
 *
 * The letter and number in a test name are the scenario id.
 */
class DifferenceCalculatorForPackageFacadesEquivalenceTest : KlibSnapshotTestCommon() {

    // ---- A. Basics ----

    @Test
    fun testA1NoChange() {
        val source = """
            fun f(x: Int) {}    
            val p: B = B()
            typealias T = Int
        """
        val old = part(source)
        assertSameDifference(old, part(source), emptySet())
        assertSameDifference(old, old, emptySet())
    }

    @Test
    fun testA2EmptyPackageClassOnly() {
        assertSameDifference(
            part("class B", withBackground = false),
            part("class B", withBackground = false),
            emptySet(),
        )
    }

    @Test
    fun testA3EmptyToAllKinds() {
        val old = part("class B", withBackground = false)
        val new = part(
            """
            class B
            fun f() {}
            val p: Int = 1
            typealias T = Int
            """,
            withBackground = false,
        )
        assertSameDifference(old, new, setOf("f", "p", "T"))

        val default = new.copy(proto = ProtoBuf.Package.getDefaultInstance())
        assertFalse(default.proto.hasTypeTable(), "Precondition: the default package has no type table")
        assertSameDifference(default, new, setOf("f", "p", "T"))
    }

    @Test
    fun testA4PrivateOnlyPackage() {
        assertSameDifference(
            part("private fun p(x: Int) {}", withBackground = false),
            part("private fun p(x: String) {}", withBackground = false),
            emptySet(),
        )
    }

    @Test
    fun testA5FunctionAddedRemovedChanged() {
        val function = part("fun f(x: Int) {}")
        assertSameDifference(part(""), function, setOf("f"))
        assertSameDifference(function, part("fun f(x: String) {}"), setOf("f"))
    }

    @Test
    fun testA6PropertyAddedRemovedChanged() {
        val property = part("val p: B = B()")
        assertSameDifference(part(""), property, setOf("p"))
        assertSameDifference(property, part("val p: B? = null"), setOf("p"))
    }

    @Test
    fun testA7TypeAliasAddedRemovedChanged() {
        val typeAlias = part("typealias T = Int")
        assertSameDifference(part(""), typeAlias, setOf("T"))
        assertSameDifference(typeAlias, part("typealias T = String"), setOf("T"))
    }

    @Test
    fun testA8Catalogue() {
        val changes = listOf(
            Triple("fParamType", "fun fParamType(x: Int) {}", "fun fParamType(x: String) {}"),
            Triple("fReturnType", "fun fReturnType(): Int = 0", "fun fReturnType(): Long = 0L"),
            Triple("fParamName", "fun fParamName(x: Int) {}", "fun fParamName(y: Int) {}"),
            Triple("fDefault", "fun fDefault(x: Int) {}", "fun fDefault(x: Int = 0) {}"),
            Triple("fInline", "fun fInline() {}", "inline fun fInline() {}"),
            Triple("fSuspend", "fun fSuspend() {}", "suspend fun fSuspend() {}"),
            Triple("fNullable", "fun fNullable(): B = B()", "fun fNullable(): B? = null"),
            Triple("fTypeParam", "fun fTypeParam(x: Int) {}", "fun <T> fTypeParam(x: Int) {}"),
            Triple("fBound", "fun <T : Any> fBound(x: T) {}", "fun <T : Comparable<T>> fBound(x: T) {}"),
            Triple("fNested", "fun fNested(x: List<Int>) {}", "fun fNested(x: List<String>) {}"),
            Triple("fVararg", "fun fVararg(x: Int) {}", "fun fVararg(vararg x: Int) {}"),
            Triple("fReceiver", "fun Int.fReceiver() {}", "fun String.fReceiver() {}"),
            Triple("fDeprecated", "fun fDeprecated() {}", "@Deprecated(\"x\") fun fDeprecated() {}"),
            Triple("fParamAnnotation", "fun fParamAnnotation(x: Int) {}", "fun fParamAnnotation(@Ann x: Int) {}"),
            Triple("fInfix", "fun Int.fInfix(x: Int) {}", "infix fun Int.fInfix(x: Int) {}"),
            Triple("plus", "fun B.plus(x: B): B = x", "operator fun B.plus(x: B): B = x"),
            Triple("fAbbrev", "fun fAbbrev(): AI = 0", "fun fAbbrev(): Int = 0"),
            Triple("fReified", "inline fun <T> fReified() {}", "inline fun <reified T> fReified() {}"),
            Triple("pType", "val pType: B = B()", "val pType: B? = null"),
            Triple("pVar", "val pVar: B = B()", "var pVar: B = B()"),
            Triple("pConst", "const val pConst: Int = 1", "const val pConst: Int = 2"),
            Triple("pGetter", "val pGetter: B = B()", "val pGetter: B get() = B()"),
            Triple("pDeprecated", "val pDeprecated: B = B()", "@Deprecated(\"x\") val pDeprecated: B = B()"),
            Triple("pReceiver", "val Int.pReceiver: B get() = B()", "val String.pReceiver: B get() = B()"),
            Triple("pLateinit", "var pLateinit: B = B()", "lateinit var pLateinit: B"),
            Triple("pSetter", "var pSetter: B = B()", "var pSetter: B = B()\n    private set"),
            Triple("AExpanded", "typealias AExpanded = Int", "typealias AExpanded = String"),
            Triple("ATypeParam", "typealias ATypeParam = List<Int>", "typealias ATypeParam<T> = List<T>"),
            Triple("ANested", "typealias ANested = List<Int>", "typealias ANested = List<String>"),
            Triple("ADeprecated", "typealias ADeprecated = Int", "@Deprecated(\"x\") typealias ADeprecated = Int"),
        )
        val unchanged = buildList {
            add("annotation class Ann")
            add("typealias AI = Int")
            repeat(8) { add("fun unchangedFun$it(x: Int, y: List<B>): B = B()") }
            repeat(6) { add("val unchangedVal$it: List<B> = emptyList()") }
            repeat(4) { add("typealias UnchangedAlias$it = Map<String, B>") }
            add("private fun unchangedPrivateFun(x: String) {}")
            add("private val unchangedPrivateVal: B = B()")
        }

        assertSameDifference(
            part((unchanged + changes.map { it.second }).joinToString("\n")),
            part((unchanged + changes.map { it.third }).joinToString("\n")),
            changes.mapTo(mutableSetOf()) { it.first },
        )
    }

    @Test
    fun testA9BodyOnly() {
        // No constant initializers: a changed constant value is stored in the metadata
        assertSameDifference(
            part(
                """
                private fun makeB(): B = B()
                fun body(): Int = 1
                val init: B = B()
                val getter: Int get() = 1
                """
            ),
            part(
                """
                private fun makeB(): B = B()
                fun body(): Int = 2
                val init: B = makeB()
                val getter: Int get() = 2
                """
            ),
            emptySet(),
        )
    }

    @Test
    fun testA10NaNConstant() {
        val source = """
            const val nanF: Float = Float.NaN
            const val nanD: Double = Double.NaN
        """
        val old = part(source)
        val new = part(source)
        for (part in listOf(old, new)) {
            assertTrue(part.constant("nanF").let { it.hasFloatValue() && it.floatValue.isNaN() }, "Precondition: nanF is stored as NaN")
            assertTrue(part.constant("nanD").let { it.hasDoubleValue() && it.doubleValue.isNaN() }, "Precondition: nanD is stored as NaN")
        }

        // A false positive that both calculators share: `checkEquals` compares the values with `!=`, and NaN != NaN
        assertSameDifference(old, new, setOf("nanF", "nanD"))
        assertSameDifference(old, old, setOf("nanF", "nanD"))
    }

    @Test
    fun testA11SignedZeroConstant() {
        val old = part("const val z: Float = 0.0f\nconst val zD: Double = 0.0")
        val new = part("const val z: Float = -0.0f\nconst val zD: Double = -0.0")
        assertEquals(0.0f.toRawBits(), old.constant("z").floatValue.toRawBits(), "Precondition: old z is 0.0f")
        assertEquals((-0.0f).toRawBits(), new.constant("z").floatValue.toRawBits(), "Precondition: new z is -0.0f")
        assertEquals(0.0.toRawBits(), old.constant("zD").doubleValue.toRawBits(), "Precondition: old zD is 0.0")
        assertEquals((-0.0).toRawBits(), new.constant("zD").doubleValue.toRawBits(), "Precondition: new zD is -0.0")

        // Both calculators hash members with `Float.hashCode()`, which tells 0.0 and -0.0 apart, so once the members are compared
        // they agree. Another changed property makes the baseline compare them.
        assertSameDifference(
            part("const val z: Float = 0.0f\nconst val zD: Double = 0.0\nconst val other = 1"),
            part("const val z: Float = -0.0f\nconst val zD: Double = -0.0\nconst val other = 2"),
            setOf("z", "zD", "other"),
        )

        // Alone, the baseline does not compare the members: its package-level `checkEquals` uses `!=`, and 0.0 == -0.0.
        // The new calculator has no such gate.
        assertEquals(emptySet(), DifferenceCalculatorForPackageFacade(old, new).difference().changedMembersNames)
        assertEquals(setOf("z", "zD"), DifferenceCalculatorForPackageFacades(listOf(old), listOf(new)).difference().changedMembersNames)
    }

    // ---- B. Gate and kind branches ----

    @Test
    fun testB1OneKindChangedOthersReordered() {
        val old = part(
            """
            fun f(x: Int) {}
            fun g() {}
            val p1: B = B()
            val p2: Int = 1
            typealias T1 = Int
            typealias T2 = String
            """
        )
        val new = part(
            """
            fun f(x: String) {}
            fun g() {}
            val p2: Int = 1
            val p1: B = B()
            typealias T2 = String
            typealias T1 = Int
            """
        )
        assertNotEquals(old.propertyNames(), new.propertyNames(), "Precondition: the properties are reordered")
        assertNotEquals(old.typeAliasNames(), new.typeAliasNames(), "Precondition: the type aliases are reordered")

        assertSameDifference(old, new, setOf("f"))
    }

    @Test
    fun testB2KindDisappears() {
        assertSameDifference(
            part("fun f() {}\nfun g() {}\nval p: Int = 1", withBackground = false),
            part("val p: Int = 1", withBackground = false),
            setOf("f", "g"),
        )
    }

    @Test
    fun testB3OnlyPrivateOfKindBecomesPublicOnes() {
        assertSameDifference(
            part("private fun a() {}\nval p: Int = 1", withBackground = false),
            part("fun a() {}\nfun b() {}\nval p: Int = 1", withBackground = false),
            setOf("a", "b"),
        )
    }

    @Test
    fun testB4KindChangeSameName() {
        assertSameDifference(part("fun foo(x: Int): B = B()"), part("val foo: B = B()"), setOf("foo"))
    }

    @Test
    fun testB5PrivateChangeInOneKindPublicInOther() {
        assertSameDifference(
            part("private fun a(x: Int) {}\nval p: B = B()"),
            part("private fun a(x: String) {}\nval p: B? = null"),
            setOf("p"),
        )
    }

    // ---- C. Privacy ----

    @Test
    fun testC1PrivateOnlyChanges() {
        assertSameDifference(
            part(
                """
                fun pub(x: Int) {}
                private fun removed() {}
                private fun changed(x: Int) {}
                private val pv: Int = 1
                private typealias PT = Int
                """
            ),
            part(
                """
                fun pub(x: Int) {}
                private fun added() {}
                private fun changed(x: String) {}
                private val pv: String = ""
                private typealias PT = String
                """
            ),
            emptySet(),
        )
    }

    @Test
    fun testC2VisibilityFlip() {
        assertSameDifference(
            part(
                """
                private fun f() {}
                internal fun g() {}
                private val h: B = B()
                private typealias A = Int
                private fun k(x: Int) {}
                """
            ),
            part(
                """
                fun f() {}
                fun g() {}
                internal val h: B = B()
                typealias A = Int
                fun k(x: String) {}
                """
            ),
            setOf("f", "g", "h", "A", "k"),
        )
    }

    // ---- D. Names ----

    private val overloads = "fun f(x: Int) {}\nfun f(x: String) {}"

    @Test
    fun testD1OverloadChanged() {
        assertSameDifference(part(overloads), part("fun f(x: Int) {}\nfun f(x: Long) {}"), setOf("f"))
    }

    @Test
    fun testD2OverloadAddedRemoved() {
        val two = part(overloads)
        assertSameDifference(two, part("$overloads\nfun f(x: Long) {}"), setOf("f"))
        assertSameDifference(two, part("fun f(x: Int) {}"), setOf("f"))
    }

    @Test
    fun testD3OverloadsSwapped() {
        assertSameDifference(part(overloads), part("fun f(x: String) {}\nfun f(x: Int) {}"), emptySet())
    }

    @Test
    fun testD4OverloadBecomesPrivate() {
        assertSameDifference(part(overloads), part("fun f(x: Int) {}\nprivate fun f(x: String) {}"), setOf("f"))
    }

    @Test
    fun testD5FunAndPropertySameName() {
        val old = part("fun foo(x: Int): B = B()\nval foo: B = B()")
        assertSameDifference(old, part("fun foo(x: String): B = B()\nval foo: B = B()"), setOf("foo"))
        assertSameDifference(old, part("fun foo(x: Int): B = B()\nval foo: B? = null"), setOf("foo"))
        assertSameDifference(old, part("fun foo(x: Int): B = B()"), setOf("foo"))
    }

    @Test
    fun testD6ThreeKindsSameName() {
        val common = "interface I\ninterface J\nfun foo(x: Int): B = B()\nval foo: B = B()"
        // The compiler rejects a type alias and a property with the same name, so the alias gets its name through the resolver
        fun partWithAliasOf(type: String): PackagePartProtoData {
            val part = part("$common\ntypealias fooAlias = $type")
            return part.withResolver(string = part.typeAlias("fooAlias").name to "foo")
        }

        val old = partWithAliasOf("I")
        assertEquals(listOf("foo"), old.typeAliasNames() - "BgAlias", "Precondition: the type alias is named foo")

        assertSameDifference(old, partWithAliasOf("J"), setOf("foo"))
    }

    @Test
    fun testD7Rename() {
        assertSameDifference(part("fun a() {}"), part("fun b() {}"), setOf("a", "b"))
    }

    // ---- E. Indexes and tables ----

    @Test
    fun testE1ReorderAndIndexShift() {
        val old = part(
            """
            class C1
            class C2
            class C3
            fun f1(x: C1): C2 = C2()
            fun f2(x: List<C3>): Map<String, C1> = emptyMap()
            fun f3(a: Int, b: String): Long = 0L
            val p1: C3 = C3()
            val p2: List<C2> = emptyList()
            var p3: String = ""
            typealias A1 = Map<C1, C2>
            typealias A2 = List<C3>
            """
        )
        val new = part(
            """
            private fun z(a: List<Map<String, B>>, zzz: Set<Double>) {}
            typealias A2 = List<C3>
            var p3: String = ""
            fun f3(a: Int, b: String): Long = 0L
            class C3
            val p2: List<C2> = emptyList()
            fun f2(x: List<C3>): Map<String, C1> = emptyMap()
            class C2
            typealias A1 = Map<C1, C2>
            fun f1(x: C1): C2 = C2()
            val p1: C3 = C3()
            class C1
            """
        )

        val names = listOf("f1", "f2", "f3")
        assertTrue(
            names.any { old.function(it).name != new.function(it).name },
            "Precondition: a string table index of a public member differs",
        )
        assertTrue(
            names.any { old.function(it).returnTypeId != new.function(it).returnTypeId },
            "Precondition: a type table index of a public member differs",
        )
        assertTrue(
            names.any { old.returnType(old.function(it)).className != new.returnType(new.function(it)).className },
            "Precondition: a class id index of a public member differs",
        )
        assertNotEquals(old.functionNames(), new.functionNames(), "Precondition: the functions are reordered")
        assertNotEquals(old.propertyNames(), new.propertyNames(), "Precondition: the properties are reordered")
        assertNotEquals(old.typeAliasNames(), new.typeAliasNames(), "Precondition: the type aliases are reordered")

        assertSameDifference(old, new, emptySet())
    }

    @Test
    fun testE2StringTableOnly() {
        val old = part("fun uniqueFn(x: Int) {}")
        val new = old.withResolver(string = old.function("uniqueFn").name to "renamedFn")
        assertTrue(new.proto === old.proto, "Precondition: the proto is the same instance")

        assertSameDifference(old, new, setOf("uniqueFn", "renamedFn"))
    }

    @Test
    fun testE3StringTableOnlyPrivate() {
        val old = part("private fun privFn(x: Int) {}")
        val new = old.withResolver(string = old.function("privFn").name to "renamedPrivFn")

        assertSameDifference(old, new, emptySet())
    }

    // No background in E4 and E5: table entries are shared, so each edited entry must belong to one member only

    @Test
    fun testE4ClassIdTableOnly() {
        val old = part(
            """
            class C1
            class C2
            fun f(): C1 = C1()
            fun g(): C2 = C2()
            private fun h(): C2 = C2()
            """,
            withBackground = false,
        )
        val classIndex = old.returnType(old.function("f")).className
        assertEquals("com/example/C1", old.nameResolver.getQualifiedClassName(classIndex), "Precondition: f returns C1")
        assertNotEquals(classIndex, old.returnType(old.function("g")).className, "Precondition: g does not use the class id of C1")
        assertNotEquals(classIndex, old.returnType(old.function("h")).className, "Precondition: h does not use the class id of C1")

        val new = old.withResolver(className = classIndex to "com/example/Other")

        assertSameDifference(old, new, setOf("f"))
    }

    @Test
    fun testE5TypeTableContentOnly() {
        val old = part(
            """
            class C1
            class C2
            fun f(): C1 = C1()
            private fun h(): C2 = C2()
            """,
            withBackground = false,
        )
        val publicTypeId = old.function("f").returnTypeId
        val privateTypeId = old.function("h").returnTypeId
        assertNotEquals(publicTypeId, privateTypeId, "Precondition: f and h use different type table entries")

        fun withNullable(typeId: Int) = old.rewriteTypeTable { setType(typeId, getType(typeId).toBuilder().setNullable(true)) }

        assertSameDifference(old, withNullable(publicTypeId), setOf("f"))
        assertSameDifference(old, withNullable(privateTypeId), emptySet())
        assertSameDifference(old, old.rewriteTypeTable { addType(getType(publicTypeId).toBuilder().setNullable(true)) }, emptySet())
    }

    @Test
    fun testE6TypeTableSameContentOtherId() {
        val old = part("fun f(): B = B()")
        val function = old.function("f")
        val newTypeId = old.proto.typeTable.typeCount
        val new = old
            .rewriteTypeTable { addType(getType(function.returnTypeId)) }
            .rewriteFunction("f") { setReturnTypeId(newTypeId) }
        assertNotEquals(function.returnTypeId, new.function("f").returnTypeId, "Precondition: the type id differs")

        assertSameDifference(old, new, emptySet())
    }

    @Test
    fun testE7InlineTypeVsTypeId() {
        val old = part("fun f(): B = B()")
        val function = old.function("f")
        assertTrue(function.hasReturnTypeId(), "Precondition: the compiled function uses the type table")

        val new = old.rewriteFunction("f") { clearReturnTypeId().setReturnType(old.returnType(function)) }
        assertEqualHashes(old, function, new, new.function("f"))

        // A false positive that both calculators share: equal hashes, but `checkEquals` compares `hasReturnType`/`hasReturnTypeId`
        assertSameDifference(old, new, setOf("f"))
    }

    @Test
    fun testE8NoTypeTable() {
        val source = """
            class B
            fun f(): B = B()
            val p: B = B()
            typealias T = B
            private fun q(): B = B()
        """
        val noTable = part(source, withBackground = false).withInlineTypes(typeTable = null)
        assertFalse(noTable.proto.hasTypeTable(), "Precondition: no type table")

        assertSameDifference(noTable, part(source, withBackground = false).withInlineTypes(typeTable = null), emptySet())
        assertSameDifference(
            noTable,
            part(source, withBackground = false).withInlineTypes(typeTable = ProtoBuf.TypeTable.getDefaultInstance()),
            emptySet(),
        )
        assertSameDifference(
            noTable,
            part(source.replace("fun f(): B = B()", "fun f(): B? = null"), withBackground = false).withInlineTypes(typeTable = null),
            setOf("f"),
        )
    }

    // ---- F. Fields both should ignore ----

    private val ignoredFieldsSource = "fun f(x: Int) {}\nval p: B = B()\ntypealias T = Int"

    private fun versionRequirementTable(version: Int): ProtoBuf.VersionRequirementTable =
        ProtoBuf.VersionRequirementTable.newBuilder().addRequirement(ProtoBuf.VersionRequirement.newBuilder().setVersion(version)).build()

    @Test
    fun testF1PackageVersionRequirementTable() {
        val old = part(ignoredFieldsSource)
        val withTable = old.rewrite { setVersionRequirementTable(versionRequirementTable(1)) }

        assertSameDifference(old, withTable, emptySet())
        assertSameDifference(withTable, old.rewrite { setVersionRequirementTable(versionRequirementTable(2)) }, emptySet())
    }

    @Test
    fun testF2ModuleName() {
        val old = part(ignoredFieldsSource)
        val withModuleName = old.rewrite { setExtension(JvmProtoBuf.packageModuleName, old.function("f").name) }
        val withOtherModuleName = old.rewrite { setExtension(JvmProtoBuf.packageModuleName, old.property("p").name) }
        assertNotEquals(old.function("f").name, old.property("p").name)

        assertSameDifference(old, withModuleName, emptySet())
        assertSameDifference(withModuleName, withOtherModuleName, emptySet())
    }

    @Test
    fun testF3LocalVariables() {
        val old = part(ignoredFieldsSource)
        val withLocalVariable = old.rewrite { addExtension(JvmProtoBuf.packageLocalVariable, old.property("p")) }
        val withOtherLocalVariable = old.rewrite { addExtension(JvmProtoBuf.packageLocalVariable, old.property("bgProp")) }

        assertSameDifference(old, withLocalVariable, emptySet())
        assertSameDifference(withLocalVariable, withOtherLocalVariable, emptySet())
    }

    @Test
    fun testF4PackageFqNameExtensions() {
        val old = part(ignoredFieldsSource)
        assertTrue(old.proto.hasExtension(KlibMetadataProtoBuf.packageFqName), "Precondition: klib package fqName is set")

        assertSameDifference(old, old.rewrite { setExtension(BuiltInsProtoBuf.packageFqName, 0) }, emptySet())
        assertSameDifference(
            old,
            old.rewrite { setExtension(KlibMetadataProtoBuf.packageFqName, old.proto.getExtension(KlibMetadataProtoBuf.packageFqName) + 1) },
            emptySet(),
        )
    }

    @Test
    fun testF5MemberVersionRequirementIndex() {
        val old = part(ignoredFieldsSource)

        assertSameDifference(old, old.rewriteFunction("f") { addVersionRequirement(0) }, setOf("f"))
    }

    @Test
    fun testF6MemberVersionRequirementSameIndex() {
        val base = part(ignoredFieldsSource).rewriteFunction("f") { addVersionRequirement(0) }
        val old = base.rewrite { setVersionRequirementTable(versionRequirementTable(1)) }
        val new = base.rewrite { setVersionRequirementTable(versionRequirementTable(2)) }

        // A false negative that both calculators share: the member stores only the index of its requirement
        assertSameDifference(old, new, emptySet())
    }

    @Test
    fun testF7SkippedMemberExtensions() {
        val old = part(ignoredFieldsSource)
        val new = old
            .rewriteFunction("f") {
                setExtension(KlibMetadataProtoBuf.functionFile, getExtension(KlibMetadataProtoBuf.functionFile) + 1)
                setExtension(KlibMetadataProtoBuf.functionKdoc, "doc")
            }
            .rewriteProperty("p") {
                setExtension(KlibMetadataProtoBuf.propertyFile, getExtension(KlibMetadataProtoBuf.propertyFile) + 1)
                setExtension(KlibMetadataProtoBuf.propertyKdoc, "doc")
            }
            .rewriteTypeAlias("T") {
                setExtension(KlibMetadataProtoBuf.typeAliasFile, getExtension(KlibMetadataProtoBuf.typeAliasFile) + 1)
                setExtension(KlibMetadataProtoBuf.typeAliasKdoc, "doc")
            }
        assertNotEquals(
            old.function("f").getExtension(KlibMetadataProtoBuf.functionFile),
            new.function("f").getExtension(KlibMetadataProtoBuf.functionFile),
        )

        assertSameDifference(old, new, emptySet())
    }

    // ---- G. Multiplicity and forced collisions ----

    private val duplicatesOld = "fun f(x: Int) {}\nprivate fun priv(x: Int) {}"

    // The changed private function makes the baseline reach its member matching for functions
    private val duplicatesNew = "fun f(x: Int) {}\nprivate fun priv(x: String) {}"

    @Test
    fun testG1DuplicatesBothSides() {
        assertSameDifference(part(duplicatesOld).withDuplicatesOf("f", 1), part(duplicatesNew).withDuplicatesOf("f", 1), emptySet())
    }

    @Test
    fun testG2DuplicatesUneven() {
        val new = part(duplicatesNew)
        assertSameDifference(part(duplicatesOld).withDuplicatesOf("f", 1), new, setOf("f"))
        assertSameDifference(part(duplicatesOld).withDuplicatesOf("f", 2), new.withDuplicatesOf("f", 1), setOf("f"))
    }

    // 31 * (31 * h + 1) + 40 == 31 * (31 * h + 2) + 9 == 31 * (31 * h + 3) - 22, whatever the interned ids are
    private val collidingVersionRequirements = listOf(listOf(1, 40), listOf(2, 9), listOf(3, -22))

    private fun PackagePartProtoData.withCollidingOverloads(vararg variants: Int): PackagePartProtoData {
        val function = function("f")
        val overloads = variants.map { variant ->
            function.toBuilder().apply { collidingVersionRequirements[variant].forEach { addVersionRequirement(it) } }.build()
        }
        val result = rewrite {
            setFunction(proto.functionList.indexOf(function), overloads.first())
            overloads.drop(1).forEach { addFunction(it) }
        }
        for (overload in overloads) assertEqualHashes(result, overloads.first(), result, overload)
        return result
    }

    @Test
    fun testG3SameKindCollisionSingle() {
        val compiled = part("fun f(x: Int) {}")
        val old = compiled.withCollidingOverloads(0)
        val new = compiled.withCollidingOverloads(1)
        assertEqualHashes(old, old.function("f"), new, new.function("f"))

        assertSameDifference(old, new, setOf("f"))
    }

    @Test
    fun testG4SameKindCollisionTwoInBucket() {
        val compiled = part("fun f(x: Int) {}")
        val old = compiled.withCollidingOverloads(0, 1)

        assertSameDifference(old, compiled.withCollidingOverloads(1, 0), emptySet())
        assertSameDifference(old, compiled.withCollidingOverloads(0), setOf("f"))
        assertSameDifference(old, compiled.withCollidingOverloads(0, 2), setOf("f"))
    }

    // ---- H. Randomized ----

    private enum class Kind { FUNCTION, PROPERTY, TYPE_ALIAS }

    private class Declaration(val kind: Kind, val name: String, val base: String, val changed: String? = null) {
        // Type arguments are dropped first, so that the comma in `Map<String, B>` is not counted
        private val parameterCount = base.substringAfter("$name(", "").substringBefore(")").replace(Regex("<[^<>]*>"), "").let { parameters ->
            if (parameters.isBlank()) 0 else parameters.split(",").size
        }

        fun findIn(part: PackagePartProtoData): MessageLite = when (kind) {
            Kind.FUNCTION -> part.proto.functionList.single {
                part.nameResolver.getString(it.name) == name && it.valueParameterCount == parameterCount
            }
            Kind.PROPERTY -> part.property(name)
            Kind.TYPE_ALIAS -> part.typeAlias(name)
        }

        override fun toString(): String = base
    }

    private val poolDeclarations = listOf(
        Declaration(Kind.FUNCTION, "f1", "fun f1(x: Int): B = B()", "fun f1(x: String): B = B()"),
        Declaration(Kind.FUNCTION, "f2", "fun f2(): B = B()", "fun f2(): B? = null"),
        Declaration(Kind.FUNCTION, "f2", "fun f2(x: Int) {}"),
        Declaration(Kind.FUNCTION, "f2", "fun f2(x: Int, y: Int) {}", "fun f2(x: Int, y: String) {}"),
        Declaration(Kind.FUNCTION, "f3", "fun f3(x: List<B>) {}", "fun f3(x: List<Int>) {}"),
        Declaration(Kind.FUNCTION, "f4", "fun f4() {}"),
        Declaration(Kind.FUNCTION, "f5", "fun f5(x: B): Int = 0", "fun f5(x: B): Long = 0L"),
        Declaration(Kind.FUNCTION, "shared", "fun shared(x: Int): B = B()", "fun shared(x: Long): B = B()"),
        Declaration(Kind.FUNCTION, "f6", "fun <T> f6(x: T) {}"),
        Declaration(Kind.FUNCTION, "f7", "fun Int.f7() {}", "fun String.f7() {}"),
        Declaration(Kind.FUNCTION, "f8", "suspend fun f8() {}", "fun f8() {}"),
        Declaration(Kind.FUNCTION, "f9", "fun f9(vararg x: Int) {}"),
        Declaration(Kind.FUNCTION, "f10", "fun f10(x: Int = 0) {}", "fun f10(x: Int) {}"),
        Declaration(Kind.FUNCTION, "f11", "fun f11(x: Map<String, B>): List<B> = emptyList()"),
        Declaration(Kind.FUNCTION, "f12", "fun f12(a: Int, b: Long, c: String) {}", "fun f12(a: Int, b: Long, c: B) {}"),
        Declaration(Kind.FUNCTION, "f13", "internal fun f13() {}", "fun f13() {}"),
        Declaration(Kind.FUNCTION, "x", "fun x(): B = B()"),
        Declaration(Kind.FUNCTION, "y", "fun y(): List<B> = emptyList()"),
        Declaration(Kind.FUNCTION, "pf1", "private fun pf1(x: Int) {}", "private fun pf1(x: String) {}"),
        Declaration(Kind.FUNCTION, "pf2", "private fun pf2() {}"),
        Declaration(Kind.FUNCTION, "pf3", "private fun pf3(x: B) {}", "fun pf3(x: B) {}"),
        Declaration(Kind.PROPERTY, "p1", "val p1: B = B()", "val p1: B? = null"),
        Declaration(Kind.PROPERTY, "p2", "val p2: List<B> = emptyList()"),
        Declaration(Kind.PROPERTY, "p3", "var p3: B = B()", "val p3: B = B()"),
        Declaration(Kind.PROPERTY, "shared", "val shared: B = B()"),
        Declaration(Kind.PROPERTY, "p4", "val p4: Int get() = 0", "val p4: Long get() = 0L"),
        Declaration(Kind.PROPERTY, "p5", "val Int.p5: B get() = B()", "val String.p5: B get() = B()"),
        Declaration(Kind.PROPERTY, "p6", "val p6: Map<String, B> = emptyMap()"),
        Declaration(Kind.PROPERTY, "p7", "lateinit var p7: B"),
        Declaration(Kind.PROPERTY, "p8", "val p8: String = f11(emptyMap()).toString()", "val p8: Int = f11(emptyMap()).size"),
        Declaration(Kind.PROPERTY, "x", "val x: B = B()"),
        Declaration(Kind.PROPERTY, "y", "val y: List<B> = emptyList()"),
        Declaration(Kind.PROPERTY, "pp1", "private val pp1: B = B()", "private val pp1: B? = null"),
        Declaration(Kind.PROPERTY, "pp2", "private val pp2: List<B> = emptyList()"),
        Declaration(Kind.TYPE_ALIAS, "A1", "typealias A1 = Int", "typealias A1 = String"),
        Declaration(Kind.TYPE_ALIAS, "A2", "typealias A2 = List<B>"),
        Declaration(Kind.TYPE_ALIAS, "A3", "typealias A3<T> = List<T>", "typealias A3<T> = Set<T>"),
        Declaration(Kind.TYPE_ALIAS, "A4", "typealias A4 = B", "typealias A4 = Map<String, B>"),
        Declaration(Kind.TYPE_ALIAS, "A5", "typealias A5 = (Int) -> B"),
        Declaration(Kind.TYPE_ALIAS, "PA", "private typealias PA = Int", "private typealias PA = String"),
    )

    private class Pool(val part: PackagePartProtoData, declarations: List<Declaration>) {
        private val members = declarations.associateWith { it.findIn(part) }

        fun partOf(declarations: List<Declaration>): PackagePartProtoData {
            val builder = ProtoBuf.Package.newBuilder().setTypeTable(part.proto.typeTable)
            for (declaration in declarations) {
                when (val member = members.getValue(declaration)) {
                    is ProtoBuf.Function -> builder.addFunction(member)
                    is ProtoBuf.Property -> builder.addProperty(member)
                    is ProtoBuf.TypeAlias -> builder.addTypeAlias(member)
                }
            }
            return part.copy(proto = builder.build())
        }
    }

    @Test
    fun testH1RandomPool() {
        val declarations = poolDeclarations
        val seed = 20261126
        val random = Random(seed)

        fun pool(ordered: List<Declaration>, changed: Set<Declaration>): Pool {
            val source = "class B\n" + ordered.joinToString("\n") { if (it in changed) it.changed!! else it.base }
            return Pool(part(source, withBackground = false), declarations)
        }

        val basePool = pool(declarations, emptySet())
        val reorderedPool = pool(declarations.reversed(), emptySet())
        // A part has one name resolver, so it cannot mix members of two compiled pools.
        // That is why changed members come from several pools, each with a few changed declarations.
        val changeable = declarations.filter { it.changed != null }
        val changedPools = List(6) { pool(declarations.shuffled(random), changeable.shuffled(random).take(3).toSet()) }

        val iterations = 2000
        var emptyDifferences = 0
        repeat(iterations) { iteration ->
            val subset = declarations.filter { random.nextBoolean() }
            val old = basePool.partOf(subset.shuffled(random))

            val sameMembers = random.nextDouble() < 0.4
            val newDeclarations: List<Declaration>
            val newPool: Pool
            if (sameMembers) {
                newDeclarations = subset.shuffled(random)
                newPool = reorderedPool
            } else {
                val edited = subset.toMutableList()
                repeat(random.nextInt(1, 4)) {
                    val notIncluded = declarations - edited.toSet()
                    if (edited.isNotEmpty() && (notIncluded.isEmpty() || random.nextBoolean())) {
                        edited.removeAt(random.nextInt(edited.size))
                    } else {
                        edited.add(notIncluded.random(random))
                    }
                }
                newDeclarations = edited.shuffled(random)
                newPool = (changedPools + reorderedPool).random(random)
            }
            val new = newPool.partOf(newDeclarations)

            val description = "seed $seed, iteration $iteration\nold: $subset\nnew: $newDeclarations\n" +
                    "new pool: ${if (newPool === reorderedPool) "reordered" else "changed #${changedPools.indexOf(newPool)}"}"
            val baseline = DifferenceCalculatorForPackageFacade(old, new).difference()
            val newResult = newDifference(old, new, baseline, description)
            assertEquals(baseline, newResult, "The calculators disagree: baseline=$baseline, new=$newResult\n$description")
            if (sameMembers) {
                assertEquals(emptySet(), baseline.changedMembersNames, "Baseline for the same members. $description")
            }

            if (baseline.changedMembersNames.isEmpty()) emptyDifferences++
        }

        assertTrue(emptyDifferences > iterations / 4, "Too few empty differences: $emptyDifferences of $iterations")
        assertTrue(iterations - emptyDifferences > iterations / 4, "Too few non-empty differences: ${iterations - emptyDifferences}")
    }

    // ---- Helpers ----

    private var sourceDirCount = 0

    /** Compiles [source] as the only file of the `com.example` package, each call in its own directory, and returns its package part. */
    private fun part(source: String, withBackground: Boolean = true): PackagePartProtoData {
        val srcDir = File(tmpDir, "src${sourceDirCount++}")
        val file = File(srcDir, "com/example/test.kt")
        file.parentFile.mkdirs()
        file.writeText("package com.example\n\n" + (if (withBackground) BACKGROUND else "") + source.trimIndent() + "\n")

        val parts = snapshotKlib(srcDir).klibFileSnapshots
            .filter { it.packageName == PACKAGE }
            .flatMap { it.classMemberLevelSnapshot!!.proto.filterIsInstance<PackagePartProtoData>() }
        check(parts.size == 1) { "Expected exactly one package part for $PACKAGE, got ${parts.size}. Source:\n${file.readText()}" }
        return parts.single()
    }

    private fun assertSameDifference(old: PackagePartProtoData, new: PackagePartProtoData, expectedNames: Set<String>) {
        assertSameDifferenceOneWay(old, new, expectedNames, "old -> new")
        assertSameDifferenceOneWay(new, old, expectedNames, "new -> old (swapped)")
    }

    private fun assertSameDifferenceOneWay(
        old: PackagePartProtoData,
        new: PackagePartProtoData,
        expectedNames: Set<String>,
        direction: String,
    ) {
        val baseline = DifferenceCalculatorForPackageFacade(old, new).difference()
        val newResult = newDifference(old, new, baseline, direction)
        assertEquals(baseline, newResult, "The calculators disagree ($direction): baseline=$baseline, new=$newResult")

        // Guards the test input: without it, a test with a wrong source would pass with two empty differences
        assertEquals(expectedNames, baseline.changedMembersNames, "Both calculators agree, but not on the expected names ($direction)")
    }

    /** Rethrows a failure of the new calculator together with the baseline result, so the report shows both. */
    private fun newDifference(old: PackagePartProtoData, new: PackagePartProtoData, baseline: Difference, description: String): Difference =
        try {
            DifferenceCalculatorForPackageFacades(listOf(old), listOf(new)).difference()
        } catch (e: Throwable) {
            throw AssertionError("The new calculator threw $e, the baseline returned $baseline ($description)", e)
        }

    private fun assertEqualHashes(
        oldPart: PackagePartProtoData,
        oldMember: MessageLite,
        newPart: PackagePartProtoData,
        newMember: MessageLite,
    ) {
        val strings = Interner<String>()
        val classIds = Interner<ClassId>()
        assertEquals(
            oldPart.memberHash(oldMember, strings, classIds),
            newPart.memberHash(newMember, strings, classIds),
            "Precondition: the two members have equal hashes",
        )
    }

    private fun PackagePartProtoData.memberHash(member: MessageLite, strings: Interner<String>, classIds: Interner<ClassId>): Int {
        val stringIndexes = { index: Int -> strings.intern(nameResolver.getString(index)) }
        val classIdIndexes = { index: Int -> classIds.intern(nameResolver.getClassId(index)) }
        val typeById = { index: Int -> proto.typeTable.getType(index) }
        return when (member) {
            is ProtoBuf.Function -> member.hashCode(stringIndexes, classIdIndexes, typeById)
            is ProtoBuf.Property -> member.hashCode(stringIndexes, classIdIndexes, typeById)
            is ProtoBuf.TypeAlias -> member.hashCode(stringIndexes, classIdIndexes, typeById)
            else -> error("Unexpected member: $member")
        }
    }

    private fun PackagePartProtoData.rewrite(block: ProtoBuf.Package.Builder.() -> Unit): PackagePartProtoData =
        copy(proto = proto.toBuilder().apply(block).build())

    private fun PackagePartProtoData.rewriteFunction(name: String, block: ProtoBuf.Function.Builder.() -> Unit): PackagePartProtoData {
        val function = function(name)
        return rewrite { setFunction(proto.functionList.indexOf(function), function.toBuilder().apply(block)) }
    }

    private fun PackagePartProtoData.rewriteProperty(name: String, block: ProtoBuf.Property.Builder.() -> Unit): PackagePartProtoData {
        val property = property(name)
        return rewrite { setProperty(proto.propertyList.indexOf(property), property.toBuilder().apply(block)) }
    }

    private fun PackagePartProtoData.rewriteTypeAlias(name: String, block: ProtoBuf.TypeAlias.Builder.() -> Unit): PackagePartProtoData {
        val typeAlias = typeAlias(name)
        return rewrite { setTypeAlias(proto.typeAliasList.indexOf(typeAlias), typeAlias.toBuilder().apply(block)) }
    }

    private fun PackagePartProtoData.rewriteTypeTable(block: ProtoBuf.TypeTable.Builder.() -> Unit): PackagePartProtoData {
        val typeTable = proto.typeTable.toBuilder().apply(block).build()
        return rewrite { setTypeTable(typeTable) }
    }

    /** The member protos stay the same instances, only what one index resolves to changes. */
    private fun PackagePartProtoData.withResolver(
        string: Pair<Int, String>? = null,
        className: Pair<Int, String>? = null,
    ): PackagePartProtoData {
        val base = nameResolver
        return copy(
            nameResolver = object : NameResolver {
                override fun getString(index: Int): String =
                    if (string != null && index == string.first) string.second else base.getString(index)

                override fun getQualifiedClassName(index: Int): String =
                    if (className != null && index == className.first) className.second else base.getQualifiedClassName(index)

                override fun isLocalClassName(index: Int): Boolean = base.isLocalClassName(index)
            }
        )
    }

    private fun PackagePartProtoData.withDuplicatesOf(name: String, count: Int): PackagePartProtoData {
        val function = function(name)
        return rewrite {
            repeat(count) {
                // The same instance added twice would collapse in the baseline's identity `HashSet`, which parsed protos never do
                val duplicate = function.toBuilder().build()
                assertNotSame(function, duplicate)
                addFunction(duplicate)
            }
        }
    }

    /** Replaces the type table ids of all members with the types themselves. Works for members without parameters and receivers. */
    private fun PackagePartProtoData.withInlineTypes(typeTable: ProtoBuf.TypeTable?): PackagePartProtoData {
        fun type(id: Int): ProtoBuf.Type = proto.typeTable.getType(id).also {
            check(it.argumentCount == 0 && !it.hasOuterTypeId() && !it.hasAbbreviatedTypeId()) { "The type has nested ids: $it" }
        }

        return rewrite {
            proto.functionList.forEachIndexed { index, function ->
                check(function.valueParameterCount == 0 && !function.hasReceiverTypeId()) { "Unsupported function: $function" }
                setFunction(index, function.toBuilder().clearReturnTypeId().setReturnType(type(function.returnTypeId)))
            }
            proto.propertyList.forEachIndexed { index, property ->
                check(!property.hasReceiverTypeId() && !property.hasSetterValueParameter()) { "Unsupported property: $property" }
                setProperty(index, property.toBuilder().clearReturnTypeId().setReturnType(type(property.returnTypeId)))
            }
            proto.typeAliasList.forEachIndexed { index, typeAlias ->
                setTypeAlias(
                    index,
                    typeAlias.toBuilder()
                        .clearUnderlyingTypeId().setUnderlyingType(type(typeAlias.underlyingTypeId))
                        .clearExpandedTypeId().setExpandedType(type(typeAlias.expandedTypeId)),
                )
            }
            if (typeTable == null) clearTypeTable() else setTypeTable(typeTable)
        }
    }

    private companion object {
        val PACKAGE = FqName("com.example")

        // Unchanged members of every kind: the baseline skips the kinds whose lists did not change, the new calculator hashes everything
        const val BACKGROUND = """class B
fun bgFun(x: Int): String = ""
val bgProp: Long = bgFun(0).length.toLong()
typealias BgAlias = List<String>
private fun bgPrivate(x: B): B = x
"""

        fun PackagePartProtoData.function(name: String): ProtoBuf.Function =
            proto.functionList.single { nameResolver.getString(it.name) == name }

        fun PackagePartProtoData.property(name: String): ProtoBuf.Property =
            proto.propertyList.single { nameResolver.getString(it.name) == name }

        fun PackagePartProtoData.typeAlias(name: String): ProtoBuf.TypeAlias =
            proto.typeAliasList.single { nameResolver.getString(it.name) == name }

        fun PackagePartProtoData.functionNames(): List<String> = proto.functionList.map { nameResolver.getString(it.name) }

        fun PackagePartProtoData.propertyNames(): List<String> = proto.propertyList.map { nameResolver.getString(it.name) }

        fun PackagePartProtoData.typeAliasNames(): List<String> = proto.typeAliasList.map { nameResolver.getString(it.name) }

        fun PackagePartProtoData.returnType(function: ProtoBuf.Function): ProtoBuf.Type = proto.typeTable.getType(function.returnTypeId)

        fun PackagePartProtoData.constant(name: String): ProtoBuf.Annotation.Argument.Value {
            val property = property(name)
            check(property.hasExtension(KlibMetadataProtoBuf.compileTimeValue)) { "Precondition: $name has a compile time value" }
            return property.getExtension(KlibMetadataProtoBuf.compileTimeValue)
        }
    }
}
