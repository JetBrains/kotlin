// DANGLING_FILE_CUSTOM_PACKAGE: bar.bar
package foo

class TopLevel {
    class Nested {
        fun funct<caret>ion(i: Int) = ""
    }
}
