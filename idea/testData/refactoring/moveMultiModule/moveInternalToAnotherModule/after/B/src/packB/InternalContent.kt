package packB

class InternalContent {
    internal fun internalFun() {}

    fun useInternalInside() {
        internalFun()
    }

    fun useInternal() {
        InternalContentUser().internalFun()
    }
}