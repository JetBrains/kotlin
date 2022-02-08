fun annotatedSwitch(str: String) =
    when {
        <expr>@Suppress("DEPRECATION")</expr>
        str.isBlank() -> null
        str.isNotEmpty() != null -> null
        else -> 1
    }
