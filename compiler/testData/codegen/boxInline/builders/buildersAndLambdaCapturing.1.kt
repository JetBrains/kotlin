import builders.*
import kotlin.InlineOption.*

inline fun testAllInline(f: () -> String) : String {
    val args = array("1", "2", "3")
    val result =
            html {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                body {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar"; +"${f()}" }
                        }
                    }
                }
            }

    return result.toString()!!
}

inline fun testHtmlNoInline(inlineOptions(ONLY_LOCAL_RETURN) f: () -> String) : String {
    val args = array("1", "2", "3")
    val result =
            htmlNoInline() {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                body {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar"; +"${f()}" }
                        }
                    }
                }
            }

    return result.toString()!!
}

inline fun testBodyNoInline(inlineOptions(ONLY_LOCAL_RETURN) f: () -> String) : String {
    val args = array("1", "2", "3")
    val result =
            html {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                bodyNoInline {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar"; +"${f()}" }
                        }
                    }
                }
            }

    return result.toString()!!
}

inline fun testBodyHtmlNoInline(inlineOptions(ONLY_LOCAL_RETURN) f: () -> String) : String {
    val args = array("1", "2", "3")
    val result =
            htmlNoInline {
                val htmlVal = 0
                head {
                    title { +"XML encoding with Kotlin" }
                }
                bodyNoInline {
                    var bodyVar = 1
                    h1 { +"XML encoding with Kotlin" }
                    p { +"this format can be used as an alternative markup to XML" }

                    // an element with attributes and text content
                    a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }

                    // mixed content
                    p {
                        +"This is some"
                        b { +"mixed" }
                        +"text. For more see the"
                        a(href = "http://jetbrains.com/kotlin") { +"Kotlin" }
                        +"project"
                    }
                    p { +"some text" }

                    // content generated from command-line arguments
                    p {
                        +"Command line arguments were:"
                        ul {
                            for (arg in args)
                                li { +arg; +"$htmlVal"; +"$bodyVar"; +"${f()}" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun box(): String {
    var expected = testAllInline({"x"});
    print(expected + " " + testHtmlNoInline({"x"}))

    if (expected != testHtmlNoInline({"x"})) return "fail 1: ${testHtmlNoInline({"x"})}\nbut expected\n${expected} "
    if (expected != testBodyNoInline({"x"})) return "fail 2: ${testBodyNoInline({"x"})}\nbut expected\n${expected} "
    if (expected != testBodyHtmlNoInline({"x"})) return "fail 3: ${testBodyHtmlNoInline({"x"})}\nbut expected\n${expected} "

    var captured = "x"
    if (expected != testHtmlNoInline({captured})) return "fail 4: ${testHtmlNoInline({captured})}\nbut expected\n${expected} "
    if (expected != testBodyNoInline({captured})) return "fail 5: ${testBodyNoInline({captured})}\nbut expected\n${expected} "
    if (expected != testBodyHtmlNoInline({captured})) return "fail 6: ${testBodyHtmlNoInline({captured})}\nbut expected\n${expected} "

    return "OK"
}