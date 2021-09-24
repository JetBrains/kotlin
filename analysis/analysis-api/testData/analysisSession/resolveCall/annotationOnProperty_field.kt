annotation class Anno

<expr>@field:Anno</expr>
var p : Int = 42
    set(value) {
        if (value > field) {
            field = value
        }
    }
