// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77301
// LANGUAGE: +ContextParameters
@DslMarker
annotation class MyMarker

@MyMarker
class WebsiteContext

class PageContext

fun postprocess(block: context(PageContext) WebsiteContext.() -> Unit) {}

context(pageContext: PageContext)
val pageContext: PageContext
    get() = pageContext

fun test() {
    postprocess {
        pageContext
    }
}
