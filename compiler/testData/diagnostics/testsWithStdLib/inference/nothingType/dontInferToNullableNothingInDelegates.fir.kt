fun <K> materialize(): K? { return null }

val x: String? by lazy { materialize() }
