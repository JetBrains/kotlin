class Foo(val name: String, val parent: Foo?, val children: List<Foo>, val isValid: Boolean) {
    fun findChild(name: String): Foo? {
        return children.find { it.name == name }
    }
}

fun test(file: Foo?, includeSelf: Boolean): List<Foo> {
    if (file == null) {
        return emptyList()
    }

    val result = ArrayList<Foo>()
    val name = file.name
    val rootFile = file.parent?.parent ?: return result
    var parentName = file.parent.name
    var prefix = parentName
    val qualifiers = prefix.indexOf('-')

    if (qualifiers != -1) {
        parentName = prefix.substring(0, qualifiers)
        prefix = prefix.substring(0, qualifiers + 1)
    } else {
        prefix += '-'
    }
    <expr>for (child in rootFile.children) {
        val n = child.name
        if ((n.startsWith(prefix) || n == parentName) && child.isValid) {
            val variation = child.findChild(name)
            if (variation != null) {
                if (!includeSelf && file == variation) {
                    continue
                }
                result.add(variation)
            }
        }
    }</expr>

    <expr_1>return result</expr_1>
}