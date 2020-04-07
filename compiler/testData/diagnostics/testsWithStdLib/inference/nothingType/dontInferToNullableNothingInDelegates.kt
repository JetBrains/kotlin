fun <K> materialize(): K? { return null }

val x: String? by lazy { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>materialize()<!> }
