annotation class Anno(val value: Int = 0)

class Owner {
    fun member() {}

    @Anno(1 + 2)
}

class Outer {
    class Nested {
        val property = 0

        @Anno private
    }
}

class Unclosed {
    @Anno(
    fun foo() {}
}

enum class Enum {
    Entry {
        @Anno
    };

    @Anno
}

@Anno
