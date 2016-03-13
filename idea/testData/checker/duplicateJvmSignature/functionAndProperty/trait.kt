interface T {
    <error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (foo(Ljava/util/List;)V):
    fun foo(l: List<Int>): Unit
    fun foo(l: List<String>): Unit">fun foo(l: List<String>)</error> {}
    <error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (foo(Ljava/util/List;)V):
    fun foo(l: List<Int>): Unit
    fun foo(l: List<String>): Unit">fun foo(l: List<Int>)</error> {}
}
