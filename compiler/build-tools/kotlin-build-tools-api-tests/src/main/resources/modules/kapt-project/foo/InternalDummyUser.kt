package foo

internal class InternalDummyUser {
    internal fun use(dummy: InternalDummy) {
        if (dummy.x != "InternalDummy.x") throw AssertionError("dummy.x = ${dummy.x}")
    }
}