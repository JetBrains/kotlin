// "Add annotation target" "true"

@Target
annotation class Ann

class Test {
    @field:Ann<caret>
    var foo = ""
}