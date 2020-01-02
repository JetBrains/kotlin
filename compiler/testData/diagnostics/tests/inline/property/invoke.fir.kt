// !DIAGNOSTICS: -UNUSED_VARIABLE -INFIX_MODIFIER_REQUIRED

inline var value: (p: Int) -> String
    get() = {"123" }
    set(s: (p: Int) -> String) {
        s(11)
        s.invoke(11)
        s invoke 11

        val z = s
    }

inline var value2: Int.(p: Int) -> String
    get() = {"123" }
    set(ext: Int.(p: Int) -> String) {
        11.ext(11)
        11.ext(11)

        val p = ext
    }