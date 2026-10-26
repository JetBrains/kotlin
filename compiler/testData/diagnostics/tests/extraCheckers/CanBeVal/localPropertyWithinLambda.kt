// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-90674

fun invokeLater(block: () -> Unit) {}

val lambda = {
    var x = 0
    ++x
}

fun test() {
    val _ = {
        var x = 0
        ++x
    }

    invokeLater {
        var x = 0
        ++x
    }

    class Local {
        val lambda = {
            var x = 0
            ++x
        }

        init {
            invokeLater {
                var x = 0
                ++x
            }
        }
    }
}

class Foo {
    val lambda = {
        var x = 0
        ++x
    }

    init {
        invokeLater {
            var x = 0
            ++x
        }
    }

    fun test() {
        val _ = {
            var x = 0
            ++x
        }

        invokeLater {
            var x = 0
            ++x
        }

        class Local {
            val lambda = {
                var x = 0
                ++x
            }

            init {
                invokeLater {
                    var x = 0
                    ++x
                }
            }
        }
    }

    class Nested {
        val lambda = {
            var x = 0
            ++x
        }

        init {
            invokeLater {
                var x = 0
                ++x
            }
        }
    }

    inner class Inner {
        val lambda = {
            var x = 0
            ++x
        }

        init {
            invokeLater {
                var x = 0
                ++x
            }
        }
    }

    companion object {
        val lambda = {
            var x = 0
            ++x
        }

        init {
            invokeLater {
                var x = 0
                ++x
            }
        }
    }

    companion {
        val lambda2 = {
            var x = 0
            ++x
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, incrementDecrementExpression, integerLiteral,
lambdaLiteral, localProperty, propertyDeclaration */
