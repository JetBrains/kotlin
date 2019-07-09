// !LANGUAGE: +NewInference

val configurations4 = listOf(
    3 to mapOf(
        2 to listOf(
            1 to listOf(
                {
                    2
                }
            )
        )
    )
)

val configurations3 = listOf(
    3 to mapOf(
        2 to listOf(
            {
                2
            }
        )
    )
)

val configurations2 = mapOf(
    2 to listOf(
        {
            2
        }
    )
)

val configurations1 = listOf(
    {
        2
    }
)