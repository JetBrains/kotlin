// DANGLING_FILE_CUSTOM_PACKAGE: bar.bar
// RESOLVE_FILE
class TopLevel {
    class Nested {
        fun function(i: Int) = ""
    }
}

val baz = 1
