suspend fun callRelease() {
    WithTypeParameter<suspend () -> Unit>()
    returnsSuspend()
    withTypeParameter<suspend () -> Unit>()

    suspendFunctionNested(listOf(suspend {  }))
    suspendFunctionNestedInFunctionType {}
    suspendFunctionType3 { x, y, z -> }

    suspendVarargs({}, {})
}
