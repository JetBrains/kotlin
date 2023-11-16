// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER

val prop: Int =
    js("1")

fun funExprBody(x: Int): Int =
    js("x")

fun funBlockBody(x: Int): Int {
    js("return x;")
}

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>returnTypeNotSepcified<!>() = js("1")
<!WRONG_JS_INTEROP_TYPE!>val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>valTypeNotSepcified<!><!> = js("1")

val a = "1"
fun nonConst(): String = "1"

val p0: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>)
val p1: Int = js(("1"))
val p2: Int = js("$<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>")
val p3: Int = js("${1}")
val p4: Int = js("${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>}${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>}")
val p5: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!> + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>)
val p6: Int = js("1" + "1")
val p7: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>nonConst()<!>)

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

val p8: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>propWithGetter<!>)

// TODO: This should be an error as property getters are no different to functions
val p9: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>propWithSimpleGetterAndInitializer<!>)
val p10: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>propWithComplexGetterAndInitializer<!>)

val p11: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>varProp<!>)
val p12: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>varPropWithSetter<!>)
val p13: Int = js(constProp)
val p14: Int = js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>delegatedVal<!>)


fun foo0(b: Boolean): Int =
    if (b) <!JSCODE_WRONG_CONTEXT!>js<!>("1") else <!JSCODE_WRONG_CONTEXT!>js<!>("2")

fun foo1(): Int {
    println()
    <!JSCODE_WRONG_CONTEXT!>js<!>("return x;")
}

fun foo11() {
    fun local1(): Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    fun local2(): Int {
        <!JSCODE_WRONG_CONTEXT!>js<!>("return 1;")
    }
    fun local3(): Int {
        println()
        <!JSCODE_WRONG_CONTEXT!>js<!>("return 1;")
    }
}

class C {
    fun memberFun1(): Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    fun memberFun2(): Int {
        <!JSCODE_WRONG_CONTEXT!>js<!>("return 1;")
    }

    constructor() {
        <!JSCODE_WRONG_CONTEXT!>js<!>("1;")
    }

    init {
        <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    }

    val memberProperty: Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")
}

fun withDefault(x: Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")) {
    println(x)
}

suspend fun suspendFun(): Int = <!JSCODE_UNSUPPORTED_FUNCTION_KIND!>js<!>("1")

inline fun inlineFun(f: () -> Int): Int = <!JSCODE_UNSUPPORTED_FUNCTION_KIND!>js<!>("f()")

fun Int.extensionFun(): Int = <!JSCODE_UNSUPPORTED_FUNCTION_KIND!>js<!>("1")

var propertyWithAccessors: Int
    get(): Int = <!JSCODE_WRONG_CONTEXT!>js<!>("1")
    set(value: Int) {
        <!JSCODE_WRONG_CONTEXT!>js<!>("console.log(value);")
    }


fun invalidNames(
    <!JSCODE_INVALID_PARAMETER_NAME!>`a b`: Int<!>,
    <!JSCODE_INVALID_PARAMETER_NAME!>`1b`: Int<!>,
    `ab$`: Int
): Int = js("1")
