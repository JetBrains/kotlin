fun foo(@Deprecated("x")
        x: Int,
        @Deprecated("y")
        @Deprecated("z")
        y: Int,
)

// SET_INT: PARAMETER_ANNOTATION_WRAP = 2
