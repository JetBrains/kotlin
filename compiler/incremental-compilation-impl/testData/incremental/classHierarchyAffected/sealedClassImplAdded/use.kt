fun use(x: Base): String =
        when (x) {
            is A -> "A"
            is B -> "B"
        }