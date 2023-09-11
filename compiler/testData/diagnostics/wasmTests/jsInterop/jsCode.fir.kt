val prop: Int =
    js("1")

fun funExprBody(x: Int): Int =
    js("x")

fun funBlockBody(x: Int): Int {
    js("return x;")
}

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>returnTypeNotSepcified<!>() = js("1")
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>valTypeNotSepcified<!> = js("1")

val a = "1"
fun nonConst(): String = "1"

val p0: Int = js(a)
val p1: Int = js(("1"))
val p2: Int = js("$a")
val p3: Int = js("${1}")
val p4: Int = js("${a}${a}")
val p5: Int = js(a + a)
val p6: Int = js("1" + "1")
val p7: Int = js(nonConst())

val propWithGetter: String
    get() = "1"

val propWithSimpleGetterAndInitializer: String = "1"
    get() = field + "2"

val propWithComplexGetterAndInitializer: String = "1"
    get() = run { field + "2" }

var varProp = "1"

var varPropWithSetter = "1"
    set(value) { field = field + value }

const val constProp = "1"

val delegatedVal: String by lazy { "1" }

val p8: Int = js(propWithGetter)

// TODO: This should be an error as property getters are no different to functions
val p9: Int = js(propWithSimpleGetterAndInitializer)
val p10: Int = js(propWithComplexGetterAndInitializer)

val p11: Int = js(varProp)
val p12: Int = js(varPropWithSetter)
val p13: Int = js(constProp)
val p14: Int = js(delegatedVal)


fun foo0(b: Boolean): Int =
    if (b) js("1") else js("2")

fun foo1(): Int {
    println()
    js("return x;")
}

fun foo11() {
    fun local1(): Int = js("1")
    fun local2(): Int {
        js("return 1;")
    }
    fun local3(): Int {
        println()
        js("return 1;")
    }
}

class C {
    fun memberFun1(): Int = js("1")
    fun memberFun2(): Int {
        js("return 1;")
    }

    constructor() {
        js("1;")
    }

    init {
        js("1")
    }

    val memberProperty: Int = js("1")
}

fun withDefault(x: Int = js("1")) {
    println(x)
}

suspend fun suspendFun(): Int = js("1")

inline fun inlineFun(f: () -> Int): Int = js("f()")

fun Int.extensionFun(): Int = js("1")

var propertyWithAccessors: Int
    get(): Int = js("1")
    set(value: Int) {
        js("console.log(value);")
    }


fun invalidNames(
    `a b`: Int,
    `1b`: Int,
    `ab$`: Int
): Int = js("1")
