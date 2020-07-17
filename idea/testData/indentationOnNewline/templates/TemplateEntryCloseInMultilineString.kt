fun a() {
    val b = 3
    val select = """
                select
                ${
                    a<caret>
                }
                from T                
                """
}

// KT-35244
// IGNORE_FORMATTER