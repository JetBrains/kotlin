// RUN_PIPELINE_TILL: FRONTEND
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
        <!DSL_SCOPE_VIOLATION!>pageContext<!>
    }
}
