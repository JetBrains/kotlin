// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-36815
// DIAGNOSTICS: -EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE -EXTENSION_SHADOWED_BY_MEMBER

// KT-36815: Wrong overload resolution for infix function call between member property+invoke without infix modifier and infix function extension

class C() {
    infix operator fun invoke(i: Int) {}  //(0)
}

class D() {
    operator fun invoke(i: Int) {}  //(1) - no infix modifier
}

class B() {
    val barC: C = C()
    val barD: D = D()
}

infix fun B.barC(i: Int) = {}  //(2)
infix fun B.barD(i: Int) = {}  //(3)

class Case() {
    val B.barC: D
        get() = D()

    infix fun B.barC(i: Int) = {}  //(4)
    infix fun B.barD(i: Int) = {}  //(5)
    infix fun B.bar(i: Int) = {}   //(6)

    fun case() {
        val b = B()
        b barC 3 // resolved to (0)
        b barD 3 // should resolve to (5), but incorrectly gives INFIX_MODIFIER_REQUIRED
        b bar 3  // resolved to (6)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, infix, integerLiteral,
lambdaLiteral, localProperty, operator, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver */
