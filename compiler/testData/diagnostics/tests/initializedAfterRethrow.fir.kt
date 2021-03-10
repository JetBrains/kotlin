fun foo(): Int = 42

object ThrowInTryWithCatch {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

    init {
        try {
            throw Exception()
        } catch (e: Exception) {
        }
        p = "OK"
    }
}

object ThrowInTryWithCatchAndFinally {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>private val p: String<!>

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
