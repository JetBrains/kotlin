// IGNORE_BACKEND: JVM_IR
open class TestProtectedCompanionInClass {
    protected companion object
}

class TestInternalCompanionInClass {
    internal companion object
}

class TestPrivateCompanionInClass {
    private companion object
}

interface TestPrivateCompanionInInterface {
    private companion object
}
