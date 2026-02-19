// DANGLING_FILE_CUSTOM_PACKAGE: bar.bar
package foo

class TopLevel {
    class Nested {
        fun func<caret>tion(i: Int) = ""
    }
}
