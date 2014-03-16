package builders

import java.util.ArrayList
import java.util.HashMap

trait Element {
    fun render(builder: StringBuilder, indent: String)

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

class TextElement(val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

abstract class Tag(val name: String) : Element {
    val children = ArrayList<Element>()
    val attributes = HashMap<String, String>()

    inline protected fun initTag<T : Element>(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}>\n")
        for (c in children) {
            c.render(builder, indent + "  ")
        }
        builder.append("$indent</$name>\n")
    }

    private fun renderAttributes(): String? {
        val builder = StringBuilder()
        for (a in attributes.keySet()) {
            builder.append(" $a=\"${attributes[a]}\"")
        }
        return builder.toString()
    }
}

abstract class TagWithText(name: String) : Tag(name) {
    fun String.plus() {
        children.add(TextElement(this))
    }
}

class HTML() : TagWithText("html") {
    inline fun head(init: Head.() -> Unit) = initTag(Head(), init)

    inline fun body(init: Body.() -> Unit) = initTag(Body(), init)

    fun bodyNoInline(init: Body.() -> Unit) = initTag(Body(), init)
}

class Head() : TagWithText("head") {
    inline fun title(init: Title.() -> Unit) = initTag(Title(), init)
}

class Title() : TagWithText("title")

abstract class BodyTag(name: String) : TagWithText(name) {
    inline fun b(init: B.() -> Unit) = initTag(B(), init)
    inline fun p(init: P.() -> Unit) = initTag(P(), init)
    inline fun pNoInline(init: P.() -> Unit) = initTag(P(), init)
    inline fun h1(init: H1.() -> Unit) = initTag(H1(), init)
    inline fun ul(init: UL.() -> Unit) = initTag(UL(), init)
    inline fun a(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
    }
}

class Body() : BodyTag("body")
class UL() : BodyTag("ul") {
    inline fun li(init: LI.() -> Unit) = initTag(LI(), init)
}

class B() : BodyTag("b")
class LI() : BodyTag("li")
class P() : BodyTag("p")
class H1() : BodyTag("h1")
class A() : BodyTag("a") {
    public var href: String
        get() = attributes["href"]!!
        set(value) {
            attributes["href"] = value
        }
}

inline fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}

fun htmlNoInline(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}