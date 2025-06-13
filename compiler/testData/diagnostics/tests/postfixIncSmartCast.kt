// RUN_PIPELINE_TILL: FRONTEND
open class I {
    operator fun inc(): ST = ST()
}

class ST : I()

fun main() {
    var local = I()
    val x: ST = <!TYPE_MISMATCH!>local++<!>
    val y: ST = <!TYPE_MISMATCH!>local<!>
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, incrementDecrementExpression, localProperty,
operator, propertyDeclaration, smartcast */
