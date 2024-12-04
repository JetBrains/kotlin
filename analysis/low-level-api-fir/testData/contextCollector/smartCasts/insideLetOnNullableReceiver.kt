fun foo(param: String?): String {
    param?.let {
        return <expr>param</expr>
    }

    return ""
}