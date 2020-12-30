@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package foo

expect interface <!LINE_MARKER("descr='Is subclassed by A'")!>Supertype<!>

class A : Supertype


