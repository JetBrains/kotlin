fun a() {
    val b = 3
    val select = """
                select
                ${
                    <caret>a}
                from T                
                """
}

// IGNORE_FORMATTER