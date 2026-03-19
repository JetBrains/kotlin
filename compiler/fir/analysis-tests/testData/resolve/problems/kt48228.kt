// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-48228
// WITH_STDLIB

// KT-48228: Ambiguous function resolution with generic parameter and trailing lambda parameter
interface Props
class Box<P: Props>

fun <P: Props> test(box: Box<P>, props: P) { println("first") }
fun <P: Props> test(box: Box<P>, block: () -> Unit) { println("second") }

fun main() {
    val box = Box<Props>()

    test(box) { } // <- should resolve to second overload
    test(box, block = { })
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
