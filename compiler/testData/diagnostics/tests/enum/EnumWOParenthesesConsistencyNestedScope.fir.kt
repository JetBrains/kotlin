class Context {

    enum class EnumerationAAA() {
        ENTRY
    }

    enum class EnumerationAAB() {
        ENTRY;
    }

    enum class EnumerationAAC() {
        ENTRY1,
        ENTRY2;
    }

    enum class EnumerationAAD() {
        ENTRY1,
        ENTRY2();
    }

    enum class EnumerationAAE() {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        abstract fun abstractFunc()
    }

    enum class EnumerationABA(arg: UserKlass = UserKlass()) {
        ENTRY
    }

    enum class EnumerationABB(arg: UserKlass = UserKlass()) {
        ENTRY;
    }

    enum class EnumerationABC(arg: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;
    }

    enum class EnumerationABD(arg: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();
    }

    enum class EnumerationABE(arg: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        abstract fun abstractFunc()
    }

    enum class EnumerationACA(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY
    }

    enum class EnumerationACB(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY;
    }

    enum class EnumerationACC(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;
    }

    enum class EnumerationACD(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();
    }

    enum class EnumerationACE(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        abstract fun abstractFunc()
    }

    enum class EnumerationADA(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
    }

    enum class EnumerationADB(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
    }

    enum class EnumerationADC(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
    }

    enum class EnumerationADD(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())

        abstract fun abstractFunc()
    }

    enum class EnumerationAEA(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())
    }

    enum class EnumerationAEB(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())
    }

    enum class EnumerationAEC(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())
    }

    enum class EnumerationAED(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())

        abstract fun abstractFunc()
    }

    enum class EnumerationBAA constructor() {
        ENTRY
    }

    enum class EnumerationBAB constructor() {
        ENTRY;
    }

    enum class EnumerationBAC constructor() {
        ENTRY1,
        ENTRY2;
    }

    enum class EnumerationBAD constructor() {
        ENTRY1,
        ENTRY2();
    }

    enum class EnumerationBAE constructor() {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        abstract fun abstractFunc()
    }

    enum class EnumerationBBA constructor(arg: UserKlass = UserKlass()) {
        ENTRY
    }

    enum class EnumerationBBB constructor(arg: UserKlass = UserKlass()) {
        ENTRY;
    }

    enum class EnumerationBBC constructor(arg: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;
    }

    enum class EnumerationBBD constructor(arg: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();
    }

    enum class EnumerationBBE constructor(arg: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        abstract fun abstractFunc()
    }

    enum class EnumerationBCA constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY
    }

    enum class EnumerationBCB constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY;
    }

    enum class EnumerationBCC constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;
    }

    enum class EnumerationBCD constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();
    }

    enum class EnumerationBCE constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        abstract fun abstractFunc()
    }

    enum class EnumerationBDA constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
    }

    enum class EnumerationBDB constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
    }

    enum class EnumerationBDC constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
    }

    enum class EnumerationBDD constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())

        abstract fun abstractFunc()
    }

    enum class EnumerationBEA constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())
    }

    enum class EnumerationBEB constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2;

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())
    }

    enum class EnumerationBEC constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1,
        ENTRY2();

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())
    }

    enum class EnumerationBED constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass()) {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg: UserKlass = UserKlass()) : this(arg, UserKlass())
        constructor() : this(UserKlass(), UserKlass())

        abstract fun abstractFunc()
    }

    enum class EnumerationCAA {
        ENTRY;

        constructor()
    }

    enum class EnumerationCAB {
        ENTRY1,
        ENTRY2;

        constructor()
    }

    enum class EnumerationCAC {
        ENTRY1,
        ENTRY2();

        constructor()
    }

    enum class EnumerationCAD {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor()

        abstract fun abstractFunc()
    }

    enum class EnumerationCBA {
        ENTRY;

        constructor(arg: UserKlass = UserKlass())
    }

    enum class EnumerationCBB {
        ENTRY1,
        ENTRY2;

        constructor(arg: UserKlass = UserKlass())
    }

    enum class EnumerationCBC {
        ENTRY1,
        ENTRY2();

        constructor(arg: UserKlass = UserKlass())
    }

    enum class EnumerationCBD {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg: UserKlass = UserKlass())

        abstract fun abstractFunc()
    }

    enum class EnumerationCCA {
        ENTRY;

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
    }

    enum class EnumerationCCB {
        ENTRY1,
        ENTRY2;

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
    }

    enum class EnumerationCCC {
        ENTRY1,
        ENTRY2();

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
    }

    enum class EnumerationCCD {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())

        abstract fun abstractFunc()
    }

    enum class EnumerationCDA {
        ENTRY;

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())
    }

    enum class EnumerationCDB {
        ENTRY1,
        ENTRY2;

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())
    }

    enum class EnumerationCDC {
        ENTRY1,
        ENTRY2();

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())
    }

    enum class EnumerationCDD {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())

        abstract fun abstractFunc()
    }

    enum class EnumerationCEA {
        ENTRY;

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())
        constructor()
    }

    enum class EnumerationCEB {
        ENTRY1,
        ENTRY2;

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())
        constructor()
    }

    enum class EnumerationCEC {
        ENTRY1,
        ENTRY2();

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())
        constructor()
    }

    enum class EnumerationCED {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        constructor(arg1: UserKlass = UserKlass(), arg2: UserKlass = UserKlass())
        constructor(arg: UserKlass = UserKlass())
        constructor()

        abstract fun abstractFunc()
    }

    enum class EnumerationDA {
        ENTRY
    }

    enum class EnumerationDB {
        ENTRY;
    }

    enum class EnumerationDC {
        ENTRY1,
        ENTRY2;
    }

    enum class EnumerationDD {
        ENTRY1,
        ENTRY2();
    }

    enum class EnumerationDE {
        ENTRY1 {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        },
        ENTRY2() {
            override fun abstractFunc() {
                TODO("Not yet implemented")
            }
        };

        abstract fun abstractFunc()
    }

}


class UserKlass
