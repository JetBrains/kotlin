// "Fix 'kotlin.browser' package usage" "true"
// JS

package test

fun use(a: Any) {}

fun usage() {
    use(kotlin.<caret>browser.localStorage.toString())
}
