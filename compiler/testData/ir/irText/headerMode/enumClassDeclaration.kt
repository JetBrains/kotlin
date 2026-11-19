// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
// HEADER_MODE
enum class A {
    EAST,
    WEST
}

enum class B {
    NORTH {
        override fun getString() = "north"
    },
    SOUTH {
        override fun getString(): String {
            return "south"
        }
    };

    abstract fun getString(): String
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
