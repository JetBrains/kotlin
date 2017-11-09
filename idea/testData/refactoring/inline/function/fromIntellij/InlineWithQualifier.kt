class Element {
    var id: String? = null

    fun <caret>getName(): String = id

    fun method(element: Element): String {
        return getName() + element.getName()
    }

    fun staticMethod(element: Element): String {
        val buffer = StringBuffer()
        buffer.append(element.getName())
        return buffer.toString()
    }

    companion object {
        fun toXML(element: Element): Element {
            val el = X("El")
            el.setAttribute("attr", element.getName())
            return el
        }
    }
}

class Usage {
    fun staticMethod(element: Element): String {
        val buffer = StringBuffer()
        buffer.append(element.getName())
        return buffer.toString()
    }
}