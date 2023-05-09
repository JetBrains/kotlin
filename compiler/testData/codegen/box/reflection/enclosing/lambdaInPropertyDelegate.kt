// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_STDLIB

package test

class C {
    val f by foo {
        {}
    }
}

fun foo(f: () -> Any): Any = f()

operator fun Any.getValue(thiz: Any?, metadata: Any?): Any = this

fun box(): String {
    // This is the class for lambda inside the `foo` call (`{}`)
    val innerLambda = C().f.javaClass

    val emInner = innerLambda.getEnclosingMethod()
    if (emInner?.getName() != "invoke") return "Fail: incorrect enclosing method for inner lambda: $emInner"

    val ecInner = innerLambda.getEnclosingClass()
    if (ecInner?.getName() != "test.C\$f\$2") return "Fail: incorrect enclosing class for inner lambda: $ecInner"

    val ectorInner = innerLambda.getEnclosingConstructor()
    if (ectorInner != null) return "Fail: inner lambda should not have enclosing constructor: $ectorInner"

    val dcInner = innerLambda.getDeclaringClass()
    if (dcInner != null) return "Fail: inner lambda should not have declaring class: $dcInner"


    // This is the class for lambda that is passed as an argument to `foo`
    val outerLambda = ecInner

    val emOuter = outerLambda.getEnclosingMethod()
    if (emOuter != null) return "Fail: outer lambda should not have enclosing method: $emOuter"

    val ecOuter = outerLambda.getEnclosingClass()
    if (ecOuter?.getName() != "test.C") return "Fail: incorrect enclosing class for outer lambda: $ecOuter"

    val ectorOuter = outerLambda.getEnclosingConstructor()
    if (ectorOuter == null) return "Fail: outer lambda _should_ have enclosing constructor"

    val dcOuter = outerLambda.getDeclaringClass()
    if (dcOuter != null) return "Fail: outer lambda should not have declaring class: $dcOuter"

    return "OK"
}
