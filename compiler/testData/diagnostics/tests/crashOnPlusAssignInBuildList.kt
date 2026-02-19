// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84061
// WITH_STDLIB

fun main() {
    buildList {
        add("O")
        <!BUILDER_INFERENCE_STUB_RECEIVER, STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY, TYPE_MISMATCH!>this[0]<!> <!OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES!>+=<!> "K"
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, thisExpression */
