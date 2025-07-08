import a.*

@Suppress("UNUSED_EXPRESSION")
private fun test(i: InternalInterface): InternalTypealias {
    PublicClass().internalMemberFun()
    PublicClass.Companion

    internalFun(internalVal)

    return i
}
