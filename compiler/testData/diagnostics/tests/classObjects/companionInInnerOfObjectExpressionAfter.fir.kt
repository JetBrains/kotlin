// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidCompanionInLocalInnerClass

val TLObjectExpression = object {
    inner class Inner {
        companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
    }
}

fun run(block: () -> Unit) {
    return block()
}

fun tlFun() {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }

    run {
        object {
            inner class Inner {
                companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
            }
        }
    }
}

val lambda = {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }
}

val anonymous = fun() {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }
}

class Class {
    var propSetGet: Int
        get() {
            object {
                inner class Inner {
                    companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
                }
            }
            return propSetGet
        }
        set(arg: Int) {
            propSetGet = arg
            object {
                inner class Inner {
                    companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
                }
            }
        }
    val propObjectExpr = object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }
    val propObjectExprNested = object {
        inner class OuterInner {
            inner class Inner {
                companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
            }
        }
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun inlineFun() {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }
}
