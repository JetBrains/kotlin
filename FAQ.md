Q: How do I run my program?

A: Define top level function `fun main(args: Array<String>)`, please ensure it's not
in a package. Also compiler switch `-entry` could be use to make any function taking
`Array<String>` and returning `Unit` be an entry point.

Q: How do I create shared library?

A: It is not possible at the moment. Currently Kotlin/Native could be used to produce either
_Kotlin/Native_ own library format, which can be statically linked with application
or an executable for target. For example, for Android targets compiler produces shared libraries
by default (as required for _Native Activity_).