// SKIP_TXT
@kotlin.coroutines.RestrictsSuspension
class RestrictedController {
    suspend fun member() {
        ext()
        member()
        memberExt()
    }

    suspend fun RestrictedController.memberExt() {
        ext()
        member()
        memberExt()
    }
}

suspend fun RestrictedController.ext() {
    ext()
    member()
    memberExt()
}

fun generate(c: suspend RestrictedController.() -> Unit) {}

fun runBlocking(x: suspend () -> Unit) {}

fun test() {
    generate a@{
        ext()
        member()
        memberExt()

        this@a.ext()
        this@a.member()
        this@a.memberExt()

        generate b@{
            ext()
            member()
            memberExt()

            this@a.ext()
            this@a.member()
            this@a.memberExt()

            this@b.ext()
            this@b.member()
            this@b.memberExt()
        }

        runBlocking {
            ext()
            member()
            memberExt()

            this@a.ext()
            this@a.member()
            this@a.memberExt()
        }
    }
}
