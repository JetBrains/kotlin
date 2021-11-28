// !LANGUAGE: -AllowSealedInheritorsInDifferentFilesOfSamePackage

sealed class TestNoSubclasses(val x: Int)

sealed class TestSubclassAfter(val x: Int)
class X1 : TestSubclassAfter(42)

sealed class TestNoSubclassesAllDefaults(val x: Int = 0)

sealed class TestSubclassAfterAllDefaults(val x: Int = 0)
class X3 : TestSubclassAfterAllDefaults()

class X4: TestSubclassBefore(1)
sealed class TestSubclassBefore(val x: Int)