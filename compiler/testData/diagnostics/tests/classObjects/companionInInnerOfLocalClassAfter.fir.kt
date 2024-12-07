// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidCompanionInLocalInnerClass

fun run(block: () -> Unit) {
    return block()
}

fun tlFun() {
    class Local {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }

    run {
        class Local {
            inner class Inner {
                companion
                <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
            }
        }
    }
}

val lambda = {
    class Local {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }
}

val anonymous = fun() {
    class Local {
        inner class Inner {
            companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
        }
    }
}

class Class {
    var propSetGet: Int
        get() {
            class Local1 {
                inner class Inner {
                    companion
                    <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
                }
            }
            return propSetGet
        }
        set(arg: Int) {
            propSetGet = arg
            class Local2 {
                inner class Inner {
                    companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_ERROR!>object<!>
                }
            }
        }
}
