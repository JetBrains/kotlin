// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A
class B
class C
class D

fun A.bar(x: Int = 0) = ""
fun A.bar(x: Int = 0, y: D.() -> Unit) = ""

fun B.bar(x: Int = 0) = ""
fun B.bar(x: Int = 0, y: D.() -> Unit) = ""

fun C.bar(x: Int = 0) = ""
fun C.bar(x: Int = 0, y: D.() -> Unit) = ""

class E {
    fun foo() {
        // `bar` calls are inapplicable since both E nor D aren't proper receivers
        // But prior to this change, every lambda was analyzed repeatedly for every candidate
        // Thus, the resulting time was exponential
        <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
            <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                    <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                        <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                            <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                    <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                        <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                            <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                                <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                                    <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                                        <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {
                                                            <!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!> {

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

