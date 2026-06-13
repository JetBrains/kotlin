fun foo(parameter: String?): String {
    parameter?.let {
        return <expr>parameter</expr>
    }

    return ""
}
