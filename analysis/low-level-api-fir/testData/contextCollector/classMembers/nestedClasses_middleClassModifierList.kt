package test

class TopLevelClass {
    <expr>private</expr> class MiddleClass {
        class NestedClass {
            fun nestedMember() = 0
        }

        fun middleMember() = 1
    }

    fun topMember() = 2
}
