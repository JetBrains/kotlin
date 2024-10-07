// LANGUAGE: +ForbidCompanionInLocalInnerClass

fun run(block: () -> Unit) {
    return block()
}

fun tlFun() {
    class Local {
        inner class Inner {
            companion object
        }
    }

    run {
        class Local {
            inner class Inner {
                companion
                object
            }
        }
    }
}

val lambda = {
    class Local {
        inner class Inner {
            companion object
        }
    }
}

val anonymous = fun() {
    class Local {
        inner class Inner {
            companion object
        }
    }
}

class Class {
    var propSetGet: Int
        get() {
            class Local1 {
                inner class Inner {
                    companion
                    object
                }
            }
            return propSetGet
        }
        set(arg: Int) {
            propSetGet = arg
            class Local2 {
                inner class Inner {
                    companion object
                }
            }
        }
}
