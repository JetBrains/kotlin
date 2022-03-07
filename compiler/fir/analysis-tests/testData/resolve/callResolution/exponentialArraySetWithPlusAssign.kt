// ISSUE: KT-50861

// only plusAssign, no set
class A {
    operator fun get(i: Int): A = this
    operator fun plusAssign(v: () -> Unit) {}
}

fun test_1(x: A) {
    x[1] += {
        someCallInsideLambda()
        x[1] += {
            someCallInsideLambda()
            x[1] += {
                someCallInsideLambda()
                x[1] += {
                    someCallInsideLambda()
                    x[1] += {
                        someCallInsideLambda()
                        x[1] += {
                            someCallInsideLambda()
                            x[1] += {
                                someCallInsideLambda()
                                x[1] += {
                                    someCallInsideLambda()
                                    x[1] += {
                                        someCallInsideLambda()
                                        x[1] += {
                                            someCallInsideLambda()
                                            x[1] += {
                                                someCallInsideLambda()
                                                x[1] += {
                                                    someCallInsideLambda()
                                                    x[1] += {
                                                        someCallInsideLambda()
                                                        x[1] += {
                                                            someCallInsideLambda()
                                                            x[1] += {
                                                                someCallInsideLambda()
                                                                Unit
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// both fit
class D {
    operator fun set(i: Int, x: D) {}
    operator fun get(i: Int): D = this
    operator fun plusAssign(x: () -> Unit) {}
    operator fun plus(v: () -> Unit): D = this
}

fun test_2(x: D) {
    x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
        someCallInsideLambda()
        x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
            someCallInsideLambda()
            x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                someCallInsideLambda()
                x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                    someCallInsideLambda()
                    x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                        someCallInsideLambda()
                        x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                            someCallInsideLambda()
                            x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                someCallInsideLambda()
                                x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                    someCallInsideLambda()
                                    x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                        someCallInsideLambda()
                                        x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                            someCallInsideLambda()
                                            x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                                someCallInsideLambda()
                                                x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                                    someCallInsideLambda()
                                                    x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                                        someCallInsideLambda()
                                                        x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                                            someCallInsideLambda()
                                                            x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
                                                                someCallInsideLambda()
                                                                Unit
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun someCallInsideLambda() {}
