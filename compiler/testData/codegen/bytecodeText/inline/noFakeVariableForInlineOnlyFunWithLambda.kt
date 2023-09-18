// WITH_STDLIB

fun test() = run {
    var tmp = 0
    "OK"
}

// JVM_TEMPLATES
// 1 LOCALVARIABLE tmp I
// 1 LOCALVARIABLE \$i\$a\$-run-NoFakeVariableForInlineOnlyFunWithLambdaKt\$test\$1 I
// 0 LDC 0
// 2 ICONST_0
// 2 ISTORE

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE tmp I
// 1 LOCALVARIABLE \$i\$a\$-run-NoFakeVariableForInlineOnlyFunWithLambdaKt\$test\$1 I
// 0 LDC 0
// 2 ICONST_0
// 2 ISTORE

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 2 LOCALVARIABLE
// 1 LOCALVARIABLE tmp\\1 I
// 1 LOCALVARIABLE \$i\$a\$-run-NoFakeVariableForInlineOnlyFunWithLambdaKt\$test\$1\\1\\3\\0 I
// 0 LDC 0
// 2 ICONST_0
// 2 ISTORE
