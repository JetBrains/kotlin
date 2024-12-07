// CHECK_TYPE_WITH_EXACT

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = Inv<UserSubklass>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class Inv<T>
open class UserKlass
open class UserSubklass: UserKlass()

// test 1: PTV is in consuming position (yield-case)
fun testYield() {
    val arg: Inv<out UserKlass> = Inv<UserSubklass>()
    val buildee = build {
        yield(arg)
    }
    checkExactType<Buildee<Inv<out UserKlass>>>(buildee)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize() {
    fun consume(arg: Inv<out UserKlass>) {}
    val buildee = build {
        consume(materialize())
    }
    checkExactType<Buildee<Inv<out UserKlass>>>(buildee)
}

fun box(): String {
    testYield()
    testMaterialize()
    return "OK"
}
