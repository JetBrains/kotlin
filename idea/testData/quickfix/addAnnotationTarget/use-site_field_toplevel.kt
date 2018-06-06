// "Add annotation target" "true"

@Target
annotation class Ann

@field:Ann<caret>
var foo = ""