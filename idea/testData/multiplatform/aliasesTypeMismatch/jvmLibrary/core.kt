package platform.lib

open class MyException

open class MyIllegalStateException : MyException()

open class MyCancellationException : MyIllegalStateException()