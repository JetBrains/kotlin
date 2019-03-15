// "Add annotation target" "false"
// ACTION: Introduce import alias
// ACTION: Make internal
// ACTION: Make private
// ACTION: Specify type explicitly
// ERROR: This annotation is not applicable to target 'top level property without backing field or delegate' and use site target '@field'

@Target
annotation class Ann

@field:Ann<caret>
var bar
    get() = ""
    set(p) {}