// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidCompanionInLocalInnerClass

val TLObjectExpression = object {
    inner class Inner {
        companion object
    }
}

fun run(block: () -> Unit) {
    return block()
}

fun tlFun() {
    object {
        inner class Inner {
            companion object
        }
    }

    run {
        object {
            inner class Inner {
                companion object
            }
        }
    }
}

val <!EXPOSED_PROPERTY_TYPE!>lambda<!> = {
    object {
        inner class Inner {
            companion object
        }
    }
}

val anonymous = fun() {
    object {
        inner class Inner {
            companion object
        }
    }
}

class Class {
    var propSetGet: Int
        get() {
            object {
                inner class Inner {
                    companion object
                }
            }
            return propSetGet
        }
        set(arg: Int) {
            propSetGet = arg
            object {
                inner class Inner {
                    companion object
                }
            }
        }
    val propObjectExpr = object {
        inner class Inner {
            companion object
        }
    }
    val propObjectExprNested = object {
        inner class OuterInner {
            inner class Inner {
                companion object
            }
        }
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun inlineFun() {
    object {
        inner <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> Inner {
            companion object
        }
    }
}
