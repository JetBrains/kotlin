// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81262
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline
// FIR_IDENTICAL

private val x = object {
    val bar = "2"
}

private fun xFun() = run {
    val z = object {
        val bar = "2"

        fun self() = this
    }
    z
}

private fun xFun2() = run {
    class Local {
        val bar = "2"

        fun self() = this
    }
    Local()
}

open class Super {
    val y: String = ""
}

internal <!NOTHING_TO_INLINE!>inline<!> fun foo() {
    val o = object : Super() {
        val x: String = ""

        init {
            x
            y
        }

        fun funfun() {
            val z = object {
                val bar = ""
            }

            z.bar
        }

        val self = this
    }

    o.x
    o.y
    o.self.x
    o.self.y

    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>x<!>.bar
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>xFun<!>().bar
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>xFun2<!>().bar
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, init, inline,
propertyDeclaration, stringLiteral */
