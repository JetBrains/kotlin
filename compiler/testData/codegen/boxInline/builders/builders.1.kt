import builders.*

fun testAllInline() : String {
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
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun testHtmlNoInline() : String {
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
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun testBodyNoInline() : String {
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
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun testBodyHtmlNoInline() : String {
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
                                li { +arg; +"$htmlVal"; +"$bodyVar" }
                        }
                    }
                }
            }

    return result.toString()!!
}

fun box(): String {
    var expected = testAllInline();

    if (expected != testHtmlNoInline()) return "fail 1: ${testHtmlNoInline()}\nbut expected\n${expected} "

    if (expected != testBodyNoInline()) return "fail 2: ${testBodyNoInline()}\nbut expected\n${expected} "

    if (expected != testBodyHtmlNoInline()) return "fail 3: ${testBodyHtmlNoInline()}\nbut expected\n${expected} "

    return "OK"
}