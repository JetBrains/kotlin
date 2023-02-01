import kotlinx.browser.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLButtonElement
import kotlinx.dom.*
import org.w3c.dom.events.MouseEvent

val a: Int by lazy { 2 }

var count = 1

const val q = 2

@JsFun("() => 'kek'")
external fun externLol(): String

fun box() : String {
    val test = document.body!! as HTMLElement
    val newElement = test.appendElement("div") {
        val inner = appendElement("div") {
            textContent = count.toString()
        }
        appendElement("button") {
            this as HTMLButtonElement
            textContent = "GO"
            onclick = {
                update()
                inner.textContent = count.toString()
                null
            }
        }
    }

    return "OK"
}

fun HTMLElement.appendElement(text: String) = appendElement(text) {}

fun update() {
    count += a
    val doc = document.body!! as HTMLElement
    doc.appendElement("h1") {
        textContent = count.toString()
    }
}

