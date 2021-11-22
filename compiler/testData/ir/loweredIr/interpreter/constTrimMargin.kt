// WITH_STDLIB
const val trimMargin = "123".trimMargin()

const val trimMarginDefault = """ABC
                |123
                |456""".trimMargin()

const val withoutMargin = """
    #XYZ
    #foo
    #bar
""".trimMargin("#")
