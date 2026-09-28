val computed: Int
    inline get() = 1

var wrapped: Int
    inline get() = 0
    inline set(value) {}

var restricted: Int
    get() = 0
    private inline set(value) {}
