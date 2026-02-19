// LANGUAGE: +AllowSealedInheritorsInDifferentFilesOfSamePackage

sealed class TestNoSubclasses(val x: Int)

sealed class TestSubclassAfter(val x: Int)
class X1 : TestSubclassAfter(42)

sealed class TestNoSubclassesAllDefaults(val x: Int = 0)

sealed class TestSubclassAfterAllDefaults(val x: Int = 0)
class X3 : TestSubclassAfterAllDefaults()

class X4: TestSubclassBefore(1)
sealed class TestSubclassBefore(val x: Int)

sealed class TestPrimaryConstructorPrivateVisibility private constructor(val x: Int)

sealed class TestPrimaryConstructorProtectedVisibility protected constructor(val x: Int)

sealed class TestSecondaryConstructorUnspecifiedVisibility(val x: Int) {
    constructor() : this(42)
}

sealed class TestSecondaryConstructorPrivateVisibility(val x: Int) {
    private constructor() : this(42)
}

sealed class TestSecondaryConstructorProtectedVisibility(val x: Int) {
    protected constructor() : this(42)
}