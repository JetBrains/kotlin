fun test() {
    js("")
    js(" ")
    js("""
               """)

    val empty = ""
    js(empty)

    val whitespace = "  "
    js(whitespace)

    val multiline = """
    """
    js(multiline)
}
