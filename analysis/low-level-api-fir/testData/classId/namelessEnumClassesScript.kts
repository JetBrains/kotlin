package one.two

/* ClassId: one/two/<no name provided> [PsiFqName: one.two.NamelessEnumClassesScript.<no name provided>] */enum class {
    /* ClassId: null */A,
    /* ClassId: null */B {
        /* ClassId: null */class InsideEntry
        /* ClassId: null */object {
            /* ClassId: null */class InsideNamelessInEntry
        }
    };

    /* ClassId: one/two/<no name provided>.Nested [PsiFqName: null] */class Nested
    /* ClassId: one/two/<no name provided>.<no name provided> [PsiFqName: null] */object
}

/* ClassId: null */object {
    /* ClassId: null */enum class NamedEnum {
        /* ClassId: null */C {
            /* ClassId: null */class InsideEntry
        };

        /* ClassId: null */class Nested
        /* ClassId: null */object {
            /* ClassId: null */class InsideNameless
        }
    }
}

/* ClassId: one/two/Outer [PsiFqName: one.two.NamelessEnumClassesScript.Outer] */class Outer {
    /* ClassId: one/two/Outer.<no name provided> [PsiFqName: one.two.NamelessEnumClassesScript.Outer.<no name provided>] */object {
        /* ClassId: one/two/Outer.<no name provided>.<no name provided> [PsiFqName: null] */enum class {
            /* ClassId: null */D;

            /* ClassId: one/two/Outer.<no name provided>.<no name provided>.Nested [PsiFqName: null] */class Nested
        }
    }
}

fun foo() {
    /* ClassId: null */enum class {
        /* ClassId: null */E {
            /* ClassId: null */class InsideEntry
        };

        /* ClassId: null */class Nested
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26896, KT-61887
