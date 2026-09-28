// RUN_PIPELINE_TILL: CODEGEN

package foo.bar

var baz = 1

fun test() {
    foo.bar.baz++
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, incrementDecrementExpression, integerLiteral,
propertyDeclaration */
