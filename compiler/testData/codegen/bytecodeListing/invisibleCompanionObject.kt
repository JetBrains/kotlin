// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField

class TestPrivateCompanion {
    private companion object Test
}

open class TestProtectedCompanion {
    protected companion object Test
}

class TestInternalCompanion {
    internal companion object Test
}