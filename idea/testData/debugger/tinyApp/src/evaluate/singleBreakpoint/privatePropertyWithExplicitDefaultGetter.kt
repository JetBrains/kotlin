package privatePropertyWithExplicitDefaultGetter

fun main(args: Array<String>) {
    val base = Some()

    //Breakpoint!
    args.size
}

annotation class Small

class Some {
    private val a: Int = 1
        @Small get

}
// EXPRESSION: base.a
// RESULT: java.lang.NoSuchMethodError : Method not found: MemberDescription(ownerInternalName = privatePropertyWithExplicitDefaultGetter/Some, name = getA, desc = ()I, isStatic = false)
