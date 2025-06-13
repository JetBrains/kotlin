// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// See KT-10061

class My {
    val x: Int? get() = 42
}

fun foo(my: My) {
    my.x!!
    when (my.x) { }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, getter, integerLiteral, nullableType,
propertyDeclaration, whenExpression, whenWithSubject */
