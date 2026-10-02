package one.two

fun foo() {
    /* ClassId: null */class Local {
        /* ClassId: null */object {
            /* ClassId: null */class A {
                /* ClassId: null */object {
                    /* ClassId: null */class B
                }
            }
        }
    }

    /* ClassId: null */class {
        /* ClassId: null */class Nested {
            /* ClassId: null */class {
                /* ClassId: null */class C
            }
        }
    }
}

/* ClassId: one/two/<no name provided> */object {
    fun bar() {
        /* ClassId: null */class Local {
            /* ClassId: null */object {
                /* ClassId: null */class D
            }
        }
    }

    val property = /* ClassId: null */object {
        /* ClassId: null */class E
    }

    init {
        /* ClassId: null */class {
            /* ClassId: null */class F
        }
    }
}
