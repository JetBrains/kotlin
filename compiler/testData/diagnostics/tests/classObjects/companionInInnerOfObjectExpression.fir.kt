// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidCompanionInLocalInnerClass

val TLObjectExpression = object {
    inner class Inner {
        companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
    }
}

fun run(block: () -> Unit) {
    return block()
}

fun tlFun() {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
        }
    }

    run {
        object {
            inner class Inner {
                companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
            }
        }
    }
}

val lambda = {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
        }
    }
}

val anonymous = fun() {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
        }
    }
}

class Class {
    var propSetGet: Int
        get() {
            object {
                inner class Inner {
                    companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
                }
            }
            return propSetGet
        }
        set(arg: Int) {
            propSetGet = arg
            object {
                inner class Inner {
                    companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
                }
            }
        }
    val propObjectExpr = object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
        }
    }
    val propObjectExprNested = object {
        inner class OuterInner {
            inner class Inner {
                companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
            }
        }
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun inlineFun() {
    object {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
        }
    }
}
