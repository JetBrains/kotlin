// KT-86380: the regenerated object inlines the crossinline lambda at the first line of
// `inlineInClassWithLambda`. The marker of that function starts at the same label as the
// lambda call. The lambda must get its own scope number, not the scope of that function.

fun <R> applyLambda(action: () -> R) = action()

inline fun <R> inlineWithCrossLambda(crossinline action: Any.() -> R) =
    applyLambda { Obj.inlineInClassWithLambda(action) }

object Obj {
    inline fun <R> inlineInClassWithLambda(action: Any.() -> R) = Any().action()
}

fun box() {
    inlineWithCrossLambda {
        "hello"
    }
}

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE \$i\$a\$-inlineWithCrossLambda-[A-Za-z]+Kt\$box\$1 I
// 1 LOCALVARIABLE \$this\$box_u24lambda_u240 Ljava/lang/Object;

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 2 LOCALVARIABLE \$i\$f\$inlineInClassWithLambda\\1\\[0-9]+ I
// 1 LOCALVARIABLE \$i\$a\$-inlineWithCrossLambda-[A-Za-z]+Kt\$box\$1\\2\\[0-9]+\\0 I
// 1 LOCALVARIABLE \$this\$box_u24lambda_u240\\2 Ljava/lang/Object;
// 0 LOCALVARIABLE \$this\$box_u24lambda_u240\\1 Ljava/lang/Object;
