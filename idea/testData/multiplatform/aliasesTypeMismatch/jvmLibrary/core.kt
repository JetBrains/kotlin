package platform.lib

open class <!LINE_MARKER("descr='Is subclassed by MyCancellationException MyIllegalStateException OtherException [jvm]'")!>MyException<!>

open class <!LINE_MARKER("descr='Is subclassed by MyCancellationException OtherException [jvm]'")!>MyIllegalStateException<!> : MyException()

open class MyCancellationException : MyIllegalStateException()