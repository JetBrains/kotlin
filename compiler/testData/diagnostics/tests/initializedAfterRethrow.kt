// FIR_IDENTICAL

fun foo(): Int = 42

object ThrowInTryWithCatch {
    private val p: String

    init {
        try {
            throw Exception()
        } catch (e: Exception) {
        }
        p = "OK"
    }
}

object ThrowInTryWithCatchAndFinally {
    private val p: String

    init {
        try {
            throw Exception()
        } catch (e: Exception) {
        } finally {
        }
        p = "OK"
    }
}

object ThrowInFinally {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

    init {
        try {
            foo()
        } catch (e: Exception) {
        } finally {
            throw Exception()
        }
        p = "OK"
    }
}

object RethrowInCatch {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            throw e
        }
        p = "OK"
    }
}

object RethrowInCatchWithFinally {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            throw e
        } finally {
        }
        p = "OK"
    }
}

object InnerTryWithCatch {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                throw e
            } catch (ee: Exception) {
            }
        }
        p = "OK"
    }
}

object InnerTryWithFinally {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                throw e
            } finally {
            }
        }
        p = "OK"
    }
}


object InnerTryWithCatchAndFinally {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                throw e
            } catch (ee: Exception) {
            } finally {
            }
        }
        p = "OK"
    }
}

object InnerCatch {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                foo()
            } catch (ee: Exception) {
                throw ee
            }
        }
        p = "OK"
    }
}

object InnerCatchWithFinally {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                foo()
            } catch (ee: Exception) {
                throw ee
            } finally {
            }
        }
        p = "OK"
    }
}

object InnerCatchOuterRethrow {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                foo()
            } catch (ee: Exception) {
                throw e
            }
        }
        p = "OK"
    }
}

object InnerCatchOuterRethrowWithFinally {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                foo()
            } catch (ee: Exception) {
                throw e
            } finally {
            }
        }
        p = "OK"
    }
}

object InnerFinally {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                foo()
            } finally {
                throw e
            }
        }
        p = "OK"
    }
}

object InnerFinallyWithCatch {
    private val p: String

    init {
        try {
            foo()
        } catch (e: Exception) {
            try {
                foo()
            } catch (ee: Exception) {
            } finally {
                throw e
            }
        }
        p = "OK"
    }
}
