// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class TestClass {
    override fun equals(@EqualityBound(TestClass::class) other: Any?): Boolean = true
}

class TestNestedClass {
    class NestedClass {
        override fun equals(@EqualityBound(NestedClass::class) other: Any?): Boolean = true
    }
}

class TestInnerClass {
    inner class InnerClass {
        override fun equals(@EqualityBound(InnerClass::class) other: Any?): Boolean = true
    }
}

interface TestInterface {
    override fun equals(@EqualityBound(TestInterface::class) other: Any?): Boolean
}

interface TestInterfaceImpl {
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>equals<!>(@EqualityBound(TestInterfaceImpl::class) other: Any?): Boolean = true
}

fun interface TestFunInterface {
    override fun equals(@EqualityBound(TestFunInterface::class) other: Any?): Boolean
}

object TestObject {
    override fun equals(@EqualityBound(TestObject::class) other: Any?): Boolean = true
}

data object TestDataObject {
    <!DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE!>override<!> fun equals(@EqualityBound(TestDataObject::class) other: Any?): Boolean = true
}

class TestCompanionObjectImplicit {
    companion object {
        override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>TestCompanionObjectImplicit<!>::class) other: Any?): Boolean = true
    }
}

class TestCompanionObjectExplicit {
    companion object {
        override fun equals(@EqualityBound(TestCompanionObjectExplicit.Companion::class) other: Any?): Boolean = true
    }
}

class TestCompanionObjectNamed {
    companion object CompanionObject {
        override fun equals(@EqualityBound(CompanionObject::class) other: Any?): Boolean = true
    }
}

object MarkerForAnonymousObject

val testAnonymousObject = object {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>MarkerForAnonymousObject<!>::class) other: Any?): Boolean {
        return super.equals(other)
    }
}

enum class TestEnum {
    ;

    <!OVERRIDING_FINAL_MEMBER!>override<!> fun equals(@EqualityBound(TestEnum::class) other: Any?): Boolean {
        return super.equals(other)
    }
}

enum class TestEnumEntry {
    Foo {
        <!OVERRIDING_FINAL_MEMBER!>override<!> fun equals(@EqualityBound(TestEnumEntry::class) other: Any?): Boolean {
            return super.equals(other)
        }
    }
}

@JvmInline
value class TestInlineValueClass(val value: String) {
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>equals<!>(@EqualityBound(TestInlineValueClass::class) other: Any?): Boolean = true
}

annotation class TestAnnotation {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>override<!> <!ANNOTATION_CLASS_MEMBER!>fun equals(@EqualityBound(TestAnnotation::class) other: Any?): Boolean<!> = true
}

object Unrelated

val any = Any()

fun testClassifiers(
    testClass: TestClass,
    testNestedClass: TestNestedClass.NestedClass,
    testInnerClass: TestInnerClass.InnerClass,
    testInterface: TestInterface,
    testFunInterface: TestFunInterface,
    testObject: TestObject,
    testCompanionObjectExplicit: TestCompanionObjectExplicit,
    testCompanionObjectNamed: TestCompanionObjectNamed,

) {
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>testClass == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == testClass<!>) {}
    if (testClass == any) {}
    if (any == testClass) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>testNestedClass == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == testNestedClass<!>) {}
    if (testNestedClass == any) {}
    if (any == testNestedClass) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>testInnerClass == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == testInnerClass<!>) {}
    if (testInnerClass == any) {}
    if (any == testInnerClass) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>testInterface == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == testInterface<!>) {}
    if (testInterface == any) {}
    if (any == testInterface) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>testFunInterface == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == testFunInterface<!>) {}

    val a = TestFunInterface { true }
    val b = TestFunInterface { false }
    if (a == b) {}
    if (b == a) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>testObject == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == testObject<!>) {}
    if (testObject == any) {}
    if (any == testObject) {}

    if (testCompanionObjectExplicit == Unrelated) {}
    if (Unrelated == testCompanionObjectExplicit) {}
    if (testCompanionObjectExplicit == any) {}
    if (any == testCompanionObjectExplicit) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>TestCompanionObjectExplicit.Companion == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == TestCompanionObjectExplicit.Companion<!>) {}
    if (TestCompanionObjectExplicit.Companion == any) {}
    if (any == TestCompanionObjectExplicit.Companion) {}

    if (testCompanionObjectNamed == Unrelated) {}
    if (Unrelated == testCompanionObjectNamed) {}
    if (testCompanionObjectNamed == any) {}
    if (any == testCompanionObjectNamed) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>TestCompanionObjectNamed.CompanionObject == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == TestCompanionObjectNamed.CompanionObject<!>) {}
    if (TestCompanionObjectNamed.CompanionObject == any) {}
    if (any == TestCompanionObjectNamed.CompanionObject) {}
}

fun testLocalClassifiers() {
    class LocalClass {
        override fun equals(@EqualityBound(LocalClass::class) other: Any?): Boolean = true
    }

    val localClass = LocalClass()
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>localClass == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>Unrelated == localClass<!>) {}

}
