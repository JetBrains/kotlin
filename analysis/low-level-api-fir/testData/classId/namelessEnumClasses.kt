package one.two

/* ClassId: one/two/<no name provided> */enum class {
    /* ClassId: null */A,
    /* ClassId: null */B {
        /* ClassId: null */class InsideEntry
        /* ClassId: null */object {
            /* ClassId: null */class InsideNamelessInEntry
        }
    };

    /* ClassId: one/two/<no name provided>.Nested */class Nested
    /* ClassId: one/two/<no name provided>.<no name provided> */object
}

/* ClassId: one/two/<no name provided> */object {
    /* ClassId: one/two/<no name provided>.NamedEnum */enum class NamedEnum {
        /* ClassId: null */C {
            /* ClassId: null */class InsideEntry
        };

        /* ClassId: one/two/<no name provided>.NamedEnum.Nested */class Nested
        /* ClassId: one/two/<no name provided>.NamedEnum.<no name provided> */object {
            /* ClassId: one/two/<no name provided>.NamedEnum.<no name provided>.InsideNameless */class InsideNameless
        }
    }
}

/* ClassId: one/two/Outer */class Outer {
    /* ClassId: one/two/Outer.<no name provided> */object {
        /* ClassId: one/two/Outer.<no name provided>.<no name provided> */enum class {
            /* ClassId: null */D;

            /* ClassId: one/two/Outer.<no name provided>.<no name provided>.Nested */class Nested
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
