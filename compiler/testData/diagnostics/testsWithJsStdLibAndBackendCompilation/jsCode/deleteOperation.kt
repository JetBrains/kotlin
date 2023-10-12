// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Any) {
    js("delete x.foo;")
    js("delete x['bar'];")
    js("delete x.baz<!JSCODE_ERROR!>()<!>;")
    js("delete <!JSCODE_ERROR!>this<!>;")
}