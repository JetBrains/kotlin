package templates

enum class Family {
    Collections
}

fun buildTemplates(): List<Family> {
    val templates = arrayListOf<Family>()
    templates.add(Collection<caret>)
    return templates
}

// ELEMENT: Family.Collections