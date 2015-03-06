package test

inline fun inline(s: () -> String): String {
    return s()
}

class InlineAll {

    inline fun inline(s: () -> String): String {
        return s()
    }

    default object {
        inline fun inline(s: () -> String): String {
            return s()
        }
    }
}