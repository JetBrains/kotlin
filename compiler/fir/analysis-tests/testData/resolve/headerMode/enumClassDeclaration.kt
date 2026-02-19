// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
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
